package io.github.alcq77.cqgent.product.starter;

import io.github.alcq77.cqgent.product.spi.tool.ProductTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class ProductPluginDirectoryToolRegistry implements InitializingBean, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(ProductPluginDirectoryToolRegistry.class);

    private final ProductStarterProperties properties;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile List<ProductTool> cachedTools = List.of();
    private final List<URLClassLoader> activeClassLoaders = new CopyOnWriteArrayList<>();
    private Thread watchThread;
    private WatchService watchService;

    public ProductPluginDirectoryToolRegistry(ProductStarterProperties properties) {
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() {
        reloadSafely();
        if (!properties.getPlugin().isEnabled()) {
            return;
        }
        try {
            Path dir = Paths.get(properties.getPlugin().getDirectory()).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            watchService = dir.getFileSystem().newWatchService();
            dir.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY);
            running.set(true);
            watchThread = Thread.ofPlatform().name("product-plugin-watch").daemon(true).start(() -> watchLoop(dir));
        } catch (Exception e) {
            log.warn("plugin watch disabled, message={}", e.getMessage());
        }
    }

    public List<ProductTool> currentTools() {
        return cachedTools;
    }

    private void watchLoop(Path dir) {
        while (running.get()) {
            try {
                WatchKey key = watchService.take();
                boolean changed = false;
                for (WatchEvent<?> ignored : key.pollEvents()) {
                    changed = true;
                }
                key.reset();
                if (changed) {
                    reloadSafely();
                    log.info("plugin directory reloaded path={}", dir);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.warn("plugin watch loop error: {}", e.getMessage());
            }
        }
    }

    private synchronized void reloadSafely() {
        if (!properties.getPlugin().isEnabled()) {
            cachedTools = List.of();
            closeClassLoaders(activeClassLoaders);
            activeClassLoaders.clear();
            return;
        }
        List<URLClassLoader> nextLoaders = new ArrayList<>();
        try {
            List<ProductTool> nextTools = loadTools(nextLoaders);
            List<URLClassLoader> previous = new ArrayList<>(activeClassLoaders);
            activeClassLoaders.clear();
            activeClassLoaders.addAll(nextLoaders);
            cachedTools = nextTools;
            closeClassLoaders(previous);
            log.info("plugin tools loaded count={}", nextTools.size());
        } catch (Exception e) {
            closeClassLoaders(nextLoaders);
            log.warn("plugin reload failed, keep previous snapshot, message={}", e.getMessage());
        }
    }

    private List<ProductTool> loadTools(List<URLClassLoader> outLoaders) throws Exception {
        Path dir = Paths.get(properties.getPlugin().getDirectory()).toAbsolutePath().normalize();
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        List<Path> jars = Files.list(dir)
                .filter(p -> p.getFileName().toString().endsWith(".jar"))
                .sorted(Comparator.comparing(Path::toString))
                .toList();
        List<ProductTool> tools = new ArrayList<>();
        for (Path jar : jars) {
            URLClassLoader cl = new URLClassLoader(new URL[]{jar.toUri().toURL()}, getClass().getClassLoader());
            outLoaders.add(cl);
            ServiceLoader.load(ProductTool.class, cl).forEach(tools::add);
        }
        return tools;
    }

    @Override
    public void destroy() {
        running.set(false);
        if (watchThread != null) {
            watchThread.interrupt();
        }
        if (watchService != null) {
            try {
                watchService.close();
            } catch (Exception ignored) {
            }
        }
        closeClassLoaders(activeClassLoaders);
        activeClassLoaders.clear();
    }

    private static void closeClassLoaders(List<URLClassLoader> classLoaders) {
        for (URLClassLoader cl : classLoaders) {
            try {
                cl.close();
            } catch (Exception ignored) {
            }
        }
    }
}
