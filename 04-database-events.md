# 缁熶竴鏅鸿兘浣?Jar 浜у搧 - 鏁版嵁涓庝簨浠惰鏄?
## 1. 褰撳墠褰㈡€?
Jar 鍐呭祵妯″紡涓嶅己鍒朵緷璧栧閮ㄦ暟鎹簱鎴栨秷鎭郴缁熴€?
- 榛樿浼氳瘽瀛樺偍锛氬唴瀛樺疄鐜帮紙`InMemoryProductSessionStore`锛?- 浜嬩欢鎬荤嚎锛氭棤寮哄埗瀹炵幇

## 2. 鎵╁睍寤鸿

褰撲笟鍔￠渶瑕佹寔涔呭寲鍜屽紓姝ュ鐞嗘椂锛屽彲閫氳繃 SPI 鎵╁睍锛?
- 浼氳瘽鎸佷箙鍖栵細瀹炵幇 `ProductSessionStore`锛圧edis/DB锛?- 浜嬩欢閫氱煡锛氬湪涓氬姟渚у寘瑁?`AgentClient` 璋冪敤骞舵姇閫?MQ

## 3. 寤鸿浜嬩欢妯″瀷锛堝彲閫夛級

- `AgentConversationStarted`
- `AgentConversationCompleted`
- `AgentToolExecuted`
- `ModelEndpointFallbackTriggered`

浠ヤ笂浜嬩欢寤鸿鐢辨帴鍏ユ柟鍦ㄥ簲鐢ㄥ眰瀹氫箟骞舵不鐞嗐€?
