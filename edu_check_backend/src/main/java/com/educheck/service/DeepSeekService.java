package com.educheck.service;

import com.educheck.entity.KnowledgeBase;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeepSeekService {
    // RestTemplate 用于发送 HTTP 请求调用 DeepSeek OpenAPI
    private final RestTemplate restTemplate;
    @Value("${deepseek.api-key}")
    private String apiKey;
    @Value("${deepseek.base-url}")
    private String baseUrl;
    @Value("${deepseek.model}")
    private String model;
    private double temperature=0.1;
    // 读取单次回答输出最大token长度
    @Value("${deepseek.max-tokens}")
    private int maxTokens;
    // 记录 key 是否有效
    private boolean keyValid = false;

    // 启动时检查 秘钥是否有效 无效降级为知识库直接匹配
    @PostConstruct
    public void init(){
        // 检测秘钥是否正常
        if(apiKey==null||apiKey.isEmpty()
                ||apiKey.contains("你的")){
            log.warn("deepseek 秘钥没有配置，后续都只能降级回答");
            keyValid=false;
            return;
        }
        try {
            // 发送请求 检测秘钥是否正常
            HttpHeaders headers=new HttpHeaders();
            headers.setBearerAuth(apiKey);
            HttpEntity<Void> entity=new HttpEntity<>(headers);
            ResponseEntity<Map> resp=restTemplate.exchange(
                    baseUrl+"/v1/models", HttpMethod.GET, entity, Map.class
            );
            keyValid=resp.getStatusCode().is2xxSuccessful();
            log.info("deepseek api 连接成功，模型:{}", model);
        } catch (Exception e) {
            log.warn("deepseek 秘钥测试失败{}，将降级匹配回答", e.getMessage());
            keyValid=false;
        }

    }
    // 用一个方法 专门返回秘钥的状态
    public boolean isKeyValid(){
        return keyValid;
    }

    // 构建系统提示词 给deepseek 加设定（分角色）
    private String buildSystemPrompt(List<KnowledgeBase> knowledge){
        StringBuilder sb=new StringBuilder();
        sb.append("你是一个校园考勤助手的智能助手。" +
                "你只回答关于校园考勤系统使用的问题。\n\n");
        sb.append("## 核心规则\n");
        sb.append("1. 必须且仅基于以下知识库内容回答" +
                "，不得使用你自身的知识\n");
        sb.append("2. 用你自己的话重新组织语言，不要逐字复制\n");
        sb.append("3. 如果问题与校园考勤完全无关" +
                "（如天气、新闻、美食、娱乐等），" +
                "请只回复：\"请咨询与校园考勤相关的问题。\"\n");
        sb.append("4. 如果知识库中没有相关信息，请只回复" +
                "：\"知识库中没有找到相关信息，请尝试换一种问法。\"\n");
        sb.append("5. 不要编造答案，不要猜测，不要使用你的内部知识\n");
        sb.append("6. 知识库中 [1] 是最相关的条目" +
                "，请以 [1] 为准回答问题，其他条目仅作参考\n");
        sb.append("7. 回答控制在200字以内\n\n");
        sb.append("## 拒绝回答的例子\n");
        sb.append("- \"今天天气怎么样\" → 请咨询与校园考勤相关的问题。\n");
        sb.append("- \"写一首诗\" → 请咨询与校园考勤相关的问题。\n");
        sb.append("- \"世界上最高的山\" → 请咨询与校园考勤相关的问题。\n");
        sb.append("- \"讲个笑话\" → 请咨询与校园考勤相关的问题。\n\n");
// 已经匹配到知识库里面的内容了
        if(knowledge!=null&&!knowledge.isEmpty()){
            // 拼接标题 告诉大模型 一下信息是从知识库读取的
            sb.append("## 知识库内容\n");
            // 循环遍历每一条检索匹配到的知识库信息
            for(int i=0;i<knowledge.size();i++){
                // 取当前循环的知识库实体
                KnowledgeBase kb=knowledge.get(i);
                // 增加分割线 让提示词更清晰
                sb.append("[").append(i+1).append("]")
                        .append(kb.getQuestion()).append("\n");
                sb.append("答:").append(kb.getAnswer()).append("\n");
            }
        }else {
            sb.append("(暂无相关知识库内容)\n");
        }
// 返回拼接的结果
        return sb.toString();
    }
    // 开始  单轮问答（不包括历史记录）
    public String ask(String question,List<KnowledgeBase> knowledge){
        return ask(question,knowledge,List.of());
    }

    // 多轮回答（包括历史记录）
    public String ask(String question,List<KnowledgeBase> knowledge,
                      List<Map<String,String>> history){
        // 判断 ds 秘钥是否正常
        if(!keyValid){
            return null;
        }
// 将知识库内容传递 构建提示词
        String systemPrompt=buildSystemPrompt(knowledge);
        try {
            // 构建请求体
            Map<String,Object> body=new HashMap<>();
            body.put("model",model);
// 设置温度参数  让大模型回答更严谨
            body.put("temperature", temperature);
// 设置回答输出的token  控制回答内容长度
            body.put("max_tokens", maxTokens);
// 构建消息列表
            List<Map<String,String>> messages=new ArrayList<>();
// 用构建好的提示词 设置系统角色
            messages.add(Map.of("role", "system", "content", systemPrompt));
            // 添加之前对话的历史记录  控制添加历史对话的条数
            int start=Math.max(0, history.size()-8);
// 将最近几条历史对话信息 添加到 新的对话里面
            for (int i=start;i<history.size();i++){
                messages.add(history.get(i));
            }
            // 把用户正在进行的提问 添加到对话内容里面
            messages.add(Map.of("role", "user", "content", question));
            body.put("messages", messages);

// 构建请求
            HttpHeaders headers=new HttpHeaders();
// 设置提交参数的格式
            headers.setContentType(MediaType.APPLICATION_JSON);
            // 把秘钥放进去
            headers.setBearerAuth(apiKey);
// 封装完整的请求
            HttpEntity<Map<String,Object>> entity=new HttpEntity<>(body,headers);
            String url=baseUrl+"/v1/chat/completions";
            // 向大模型发送请求
            ResponseEntity<Map> response=restTemplate.exchange(url,HttpMethod.POST,entity,Map.class);
// 解析返回结果
// 大模型返回的 body 有内容
            if(response.getBody()!=null){
                // 获取 问答候选列表
                List<Map> choice=(List<Map>)response.getBody().get("choice");
                // 返回值 choice 部分不为空
                if(choice!=null&&!choice.isEmpty()){
                    Map<String,Object> message=(Map<String,Object>)choice.get(0).get("message");

                    // 从 message 里面拿到对应的 文字内容
                    String content=(String) message.get("content");
// 最后提取的内容不为空（去掉前后空格） 为空（返回空）
                    return content!=null?content.trim():null;
                }
            }
            return null;
        } catch (Exception e) {
            log.error("deepseek api 调用失败:{}",e.getMessage());
            return null;
        }

    }
    /** 检测用户意图（请假等），返回结构化JSON或null */
    public Map<String, Object> detectAction(String question, List<Map<String, String>> history) {
        if (!keyValid) return null;
        try {
            String systemPrompt =
                "你是一个校园系统的请假助手。根据整个对话历史判断是否是请假申请流程。如果是从所有对话中提取：\n" +
                "action=apply_leave type=sick/personal/official/annual startDate=YYYY-MM-DD HH:mm endDate reason(事由)\n" +
                "格式: {\"action\":\"apply_leave\",\"type\":\"sick\",\"startDate\":\"2026-07-28 14:00\",\"endDate\":\"2026-07-29 18:00\",\"reason\":\"感冒发烧\"}\n" +
                "type可选:sick(病假) personal(事假) official(公假) annual(年假)\n" +
                "日期格式: YYYY-MM-DD HH:mm（用当前日期推算相对时间）\n" +
                "如果缺少必要信息（类型、时间、事由），返回: {\"action\":\"clarify\",\"message\":\"请提供XX信息\"}\n" +
                "如果缺少必要信息（类型、时间、事由任一项），返回: action=clarify message=需要补充的具体信息\n" +
                "只返回JSON，不要额外文字。对话中AI的追问和用户的回复都是有效信息来源，结合最新回复更新已收集的字段。";

            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("temperature", 0.1);
            body.put("max_tokens", 256);
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            int start = Math.max(0, history.size() - 4);
            for (int i = start; i < history.size(); i++) messages.add(history.get(i));
            messages.add(Map.of("role", "user", "content", question));
            body.put("messages", messages);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> resp = restTemplate.exchange(baseUrl + "/v1/chat/completions", HttpMethod.POST, entity, Map.class);
            // Fix: deepseek response key is "choices" (plural), but the existing code uses "choice"
            String choiceKey = resp.getBody() != null && resp.getBody().containsKey("choices") ? "choices" : "choice";
            List<Map> choice = resp.getBody() != null ? (List<Map>) resp.getBody().get(choiceKey) : null;
            if (choice != null && !choice.isEmpty()) {
                Map<String, Object> msg = (Map<String, Object>) choice.get(0).get("message");
                String content = (String) msg.get("content");
                if (content != null && (content.contains("apply_leave") || content.contains("clarify") || content.contains("none"))) {
                    if (content.startsWith("```")) {
                        content = content.replaceAll("```json\s*|```\s*", "").trim();
                    }
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    return mapper.readValue(content, Map.class);
                }
            }
            return null;
        } catch (Exception e) {
            log.warn("意图识别失败: {}", e.getMessage());
            return null;
        }
    }
}