# AI Agent Java SDK (Preview)

[CI Compile Check](https://github.com/alcq77/agent-platform-java-sdk/actions/workflows/ci-compile.yml)

娑撯偓娑擃亪娼伴崥?Java 閻ㄥ嫯浜ら柌?Agent 瀵偓閸欐垵浼愰崗宄板瘶閵? 
閻╊喗鐖ｉ弰顖濐唨妞ゅ湱娲伴柅姘崇箖 **SDK / Spring Boot Starter** 閺傜懓绱¤箛顐︹偓鐔兼肠閹?Agent 閼宠棄濮忛敍宀冣偓灞肩瑝閺勵垰鍘涢幖顓濈閺佹潙顨滈悪顒傜彌楠炲啿褰撮妴?
> 瑜版挸澧犳禒鎾崇氨娴犲秴婀幈銏も偓鐔诲嚡娴狅絼鑵戦敍灞肩窗缂佈呯敾娴兼ê瀵查妴鍌涱偨鏉╁骸寮芥＃鍫濇嫲瀵ら缚顔呴妴?
## Why This Project

- 鐏忎粙鍣虹亸鎴滅贩鐠ф牭绱版导妯哄帥閸愬懎绁甸懗钘夊閿涘苯鍣虹亸鎴︻杺婢舵牠鍎寸純鍙夊灇閺?- 閺勬挻澧跨仦鏇窗濡€崇€烽妴浣镐紣閸忔灚鈧椒绱扮拠婵嗙摠閸屻劑鍏橀幓鎰返 SPI
- 閸欘垱甯堕敍姘暜閹镐浇鐭鹃悽渚库偓浣哄晬閺傤厹鈧礁浼愰崗鐤殶閻劌鐣ㄩ崗銊︾墡妤?- 閸欘垵鎯ら崷甯窗閹绘劒绶?Starter 閸滃瞼銇氭笟瀣紣缁嬪绱濇笟澶哥艾閻╁瓨甯撮幒銉ュ弳

## Features

- 濡€崇€风捄顖滄暠閿涘牓鈧槒绶Ο鈥崇€烽妴浣峰瘜婢跺洢鈧礁濮為弶鍐︹偓浣镐淮鎼撮攱鍔呴惌銉礆
- 閹?`taskType/tags` 閻ㄥ嫬顦垮Ο鈥崇€烽崝銊︹偓浣稿瀻濞翠緤绱橮1閿?- 鏉╂劘顢戦弮鎯邦潎濞村绱癭EmbeddedAgentClient.runtimeMetrics()` 閸氬牆鑻熼悢鏃€鏌?鐠侯垳鏁辨径杈Е閸掑棛琚妴涔romptTemplates` 閹垫挾鍋ｉ敍灞间簰閸?LangChain4j
  瀹搞儱鍙跨仦鍌滄畱閸氬本顒?濞翠礁绱＄拫鍐暏濞嗏剝鏆熼妴涔oolInvocations`閵嗕梗toolValidationFailures`閵嗕梗toolExecutionFailures`
- 閸╄桨绨?LangChain4j 閻?Agent 娑撳鎽兼稉?tool-calling 闁倿鍘ょ仦?- Advisor/Interceptor 缂佺喍绔存晶鐐插繁闁炬拝绱欓幓鎰仛濞夈劌鍙嗛妴浣风瑐娑撳鏋冪憗浣稿閵嗕礁鐣ㄩ崗銊ㄧ箖濠娿們鈧礁顓哥拋鈥冲讲閹绘帗瀚堥敍?- RAG 閸╄櫣顢呴懗钘夊閿涘牊鏋冨锝咁嚤閸忋儯鈧礁鍨忛崚鍡愨偓涔猰bedding閵嗕焦顥呯槐顫偓浣风瑐娑撳鏋冨▔銊ュ弳閿?- RAG 閺堫剙婀撮惌銉ㄧ槕鎼存挾娲拌ぐ鏇烆嚤閸忋儻绱檂workspace/knowledge` 娑?`md/txt`閿?- RAG 婢х偤鍣虹槐銏犵穿娑撳孩绔婚崡鏇熷瘮娑斿懎瀵查敍鍧剋orkspace/rag/index-manifest.json`閿?- Starter 閺€顖涘瘮 `agent.product.rag.*` 闁板秶鐤嗛崠鏍殰閸斻劏顥婇柊?RAG Advisor閿涘牆鎯?metadata/source 鏉╁洦鎶ら敍?- RAG 閻戭厽娲块弬甯窗閸氼垰濮╅崥搴＄暰閺冭泛顤冮柌蹇撳煕閺傚府绱遍崑銉ユ倣濡偓閺屻儴绶崙鐑樻瀮濡楋絾鏆?閸掑棗娼￠弫?閺堚偓閸氬海鍌ㄥ鏇熸闂?- 娴兼俺鐦界粻锛勬倞閿涘牆鍞寸純顔煎敶鐎?閺傚洣娆㈢化鑽ょ埠 + Redis/JDBC 鐎涙ê鍋嶉敍?- 閹绘帊娆㈡稉搴㈠Η閼崇晫娲拌ぐ鏇炲鏉炴枻绱檞orkspace + plugins + skills閿?- Spring Boot 閼奉亜濮╃憗鍛村帳娑撳骸浠存惔閿嬵梾閺屻儻绱欓崥?`cqagentSessionStore` Actuator 缁旑垳鍋ｉ敍?
## Quick Start

- 娑撹鍙嗛崣锝忕窗`[docs/product/quick-start-10min.md](docs/product/quick-start-10min.md)`

### Spring Boot Starter閿涘牐顕涚紒鍡欏閿?
鐟欎緤绱癭[docs/product/quick-start-starter.md](docs/product/quick-start-starter.md)`

### Java SDK閿涘牐顕涚紒鍡欏閿?
鐟欎緤绱癭[docs/product/quick-start-sdk.md](docs/product/quick-start-sdk.md)`

## Examples

- `examples/java-sdk-demo`閿涙氨鍑?Java 閺堚偓鐏忓繒銇氭笟?- `examples/spring-starter-demo`閿涙瓔pring Boot 閺堚偓鐏忓繒銇氭笟?
## Project Layout

> 鐠囧瓨妲戦敍姘瘜瀹搞儳鈻奸惄顔肩秿娑?`cq-agent-parent/cq-agent`閵?
- `cqagent-java-sdk`閿涙瓔DK閿涘潉cq-agent-parent/cq-agent/cqagent-java-sdk`閿?- `cqagent-spring-boot-starter`閿涙瓔tarter閿涘潉cq-agent-parent/cq-agent/cqagent-spring-boot-starter`閿?- `cqagent-core-engine`閿涙艾鐔€娴?LangChain4j 閻ㄥ嫭鐗宠箛鍐箥鐞涘本妞傞敍鍧刢q-agent-parent/cq-agent/cqagent-core-engine`閿?- `cqagent-spi`閿涙碍澧跨仦鏇熷复閸欙綇绱檂cq-agent-parent/cq-agent/cqagent-spi`閿?- `cqagent-plugins`閿涙碍褰冩禒鍓併仛娓氬绱檂cq-agent-parent/cq-agent/cqagent-plugins`閿?
## Build

Requirements:

- JDK 21+
- Maven 3.9+

```bash
cd cq-agent-parent
mvn -q clean compile -DskipTests
```

## Code Conventions

- 閺傛澘顤冩潻鎰攽閺冨爼鎽肩捄顖欏敩閻焦妞傞敍灞肩喘閸忓牐藟姒绘劗琚?閺傝纭跺▔銊╁櫞閿涘矁顕╅弰搴樷偓婊嗕捍鐠愶綀绔熼悾?+ 鐠嬪啰鏁ゆい鍝勭碍閳?- 閸忔娊鏁崚鍡樻暜閿涘牐鐭鹃悽渚库偓渚€鍣哥拠鏇樷偓浣哄晬閺傤厹鈧礁浼愰崗鐤殶閻㈩煉绱氭晶鐐插缁犫偓閻叀顢戦崘鍛暈闁插绱濇笟澶哥艾閹烘帡娈?- 鐎电懓顦?SPI 娣囨繃瀵旂拠顓濈疅缁嬪啿鐣鹃敍娑氱壃閸у繑鈧嗙殶閺佹挳娓剁紒娆忓毉閸忕厧顔愮仦鍌涘灗鏉╀胶些閹绘劗銇?
## Docs

- [闁板秶鐤嗛崣鍌濃偓鍍?docs/product/config-reference.md)
- [10閸掑棝鎸撻幒銉ュ弳閹稿洤宕(docs/product/quick-start-10min.md)
- [閺嶇绺鹃崝鐔诲厴閸╄櫣鍤嶿(docs/product/core-capabilities.md)
- [Current Roadmap](docs/product/current-roadmap.md)
- [API 閸欏倽鈧儩(docs/product/api-reference.md)
- [SPI 閹碘晛鐫峕(docs/product/spi-extension.md)
- [Workspace 鐟欏嫯瀵朷(docs/product/workspace-layout.md)
- [鏉╀胶些閹稿洤宕(docs/product/migration-guide.md)
- [閸欐垵绔峰▽鑽ゆ倞](docs/product/release-governance.md)

## Roadmap

- 閺囨潙鐣崰鍕畱瀹搞儱鍙跨拫鍐暏閸楀繗顔呮稉搴ゎ潎濞?- 閺囧绮忕划鎺戝閻ㄥ嫯鐭鹃悽鍙樼瑢閻旀梹鏌囩粵鏍殣
- Redis / DB 娴兼俺鐦界€涙ê鍋嶉幓鎺嶆
- 閺囨潙鐣弫瀵告畱缁€杞扮伐娑撳氦鍤滈崝銊ュ濞村鐦?
## Contributing

濞嗐垼绻嬮柅姘崇箖 Issue / PR 閹绘劕缂撶拋顔衡偓? 
婵″倹鐏夋担鐘叉躬閹恒儱鍙嗘潻鍥┾柤娑擃參浜ｉ崚浼存６妫版﹫绱濇稊鐔割偨鏉╁海娲块幒銉﹀絹娑撯偓娑擃亝娓剁亸蹇擃槻閻滆埇鈧?


