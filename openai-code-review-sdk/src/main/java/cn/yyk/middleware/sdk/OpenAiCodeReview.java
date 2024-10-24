package cn.yyk.middleware.sdk;

import cn.yyk.middleware.sdk.domain.model.ChatCompletionRequest;
import cn.yyk.middleware.sdk.domain.model.ChatCompletionSyncResponse;
import cn.yyk.middleware.sdk.domain.model.Message;
import cn.yyk.middleware.sdk.domain.model.Model;
import cn.yyk.middleware.sdk.utils.BearerTokenUtils;
import cn.yyk.middleware.sdk.utils.WXAccessTokenUtils;
import com.alibaba.fastjson2.JSON;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.util.Scanner;

/**
 * @BelongsProject: openai-code-review-yyk
 * @BelongsPackage: cn.yyk.middleware.sdk
 * @Author: yueyueking
 * @CreateTime: 2024-10-23  13:21
 * @Description: TODO
 * @Version: 1.0
 */
public class OpenAiCodeReview {
    public static void main(String[] args) throws Exception {
        System.out.println("openai代码评审，测试执行");
//        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
        //这个要到github里面设置一个token，主要是方便得到token，有过期值的，如果失效了记得再弄一个
        String token = System.getenv("GITHUB_TOKEN");
//        String token = "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";
        if (null == token || token.isEmpty()) {
            throw new RuntimeException("token is null");
        }

        //1、代码检出
        ProcessBuilder processBuilder = new ProcessBuilder("git", "diff", "HEAD~1", "HEAD");
        processBuilder.directory(new File("."));
        Process process = processBuilder.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        StringBuilder diffCode = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            diffCode.append(line);
        }
        int exitCode = process.waitFor();
        System.out.println("Exited with code:" + exitCode);
        System.out.println("评审代码：" + diffCode.toString());

        // 2. chatglm 代码评审
        String log = codeReview(diffCode.toString());
        System.out.println("code review" + log);

        // 3. 写入评审日志
        String logUrl = writeLog(token, log);
        System.out.println("writeLog：" + logUrl);

        // 4. 消息通知
        System.out.println("pushMessage：" + logUrl);
        pushMessage(logUrl);
        System.out.println("主函数内执行1");
    }

    private static void pushMessage(String logUrl){
        String accessToken = WXAccessTokenUtils.getAccessToken();
        System.out.println(accessToken);
        System.out.println("主函数内执行2");
        Message message = new Message();
        message.put("project","big-market");
        message.put("review","logUrl");
        message.setUrl(logUrl);
        message.setTemplate_id("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");
        String url = String.format("https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=%s", accessToken);
        sendPostRequest(url, JSON.toJSONString(message));
    }
    //工具类
    private static void sendPostRequest(String urlString, String jsonBody) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            try (Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8.name())) {
                String response = scanner.useDelimiter("\\A").next();
                System.out.println(response);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String codeReview(String diffCode) throws Exception{
        String apiKeySecret = "c49242ee95058932c2b1036cc782a483.tToE3L2PmUR3xdnh";
        String token = BearerTokenUtils.getToken(apiKeySecret);

        URL url = new URL("https://open.bigmodel.cn/api/paas/v4/chat/completions");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Bearer " + token);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");
        connection.setDoOutput(true);


        ChatCompletionRequest chatCompletionRequest = new ChatCompletionRequest();
        chatCompletionRequest.setModel(Model.GLM_4_FLASH.getCode());
        chatCompletionRequest.setMessages(new ArrayList<ChatCompletionRequest.Prompt>(){
            private static final long serialVersionUID = -7988151926241837899L;
            {
                add(new ChatCompletionRequest.Prompt("user", "你是一个高级编程架构师，精通各类场景方案、架构设计和编程语言请，请您根据git diff记录，对代码做出评审。代码如下:"));
                add(new ChatCompletionRequest.Prompt("user", diffCode));
            }
        });

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = JSON.toJSONString(chatCompletionRequest).getBytes(StandardCharsets.UTF_8);
            os.write(input);
        }

        int responseCode = connection.getResponseCode();
        System.out.println(responseCode);

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;

        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null){
            content.append(inputLine);
        }

        in.close();
        connection.disconnect();
        System.out.println(content.toString());
        ChatCompletionSyncResponse response = JSON.parseObject(content.toString(), ChatCompletionSyncResponse.class);
//        System.out.println(response.getChoices().get(0).getMessage().getContent());
        return response.getChoices().get(0).getMessage().getContent();
    }

    private static String writeLog(String token, String log) throws Exception{
        Git git = Git.cloneRepository()
                .setURI("https://github.com/XiaoYueYueKing/openai-code-review-log.git")
                .setDirectory(new File("repo"))
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(token, ""))
                .call();

        String dateFolderName = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

        File dateFolder = new File("repo/" + dateFolderName);
        if (!dateFolder.exists()) {
            dateFolder.mkdirs();
        }
        //随便找个能生成随机文件名的工具包
        String fileName = generateRandomString(12) + ".md";
        File newFile = new File(dateFolder, fileName);
        try (FileWriter writer = new FileWriter(newFile)) {
            writer.write(log);
        }
        //调用一下git的命令
        git.add().addFilepattern(dateFolderName + "/" + fileName).call();
        git.commit().setMessage("Add new file via GitHub Actions").call();
        git.push().setCredentialsProvider(new UsernamePasswordCredentialsProvider(token, "")).call();

        System.out.println("Changes have been pushed to the repository.");

        return "https://github.com/XiaoYueYueKing/openai-code-review-log/blob/master/" + dateFolderName + "/" + fileName;
    }

    private static String generateRandomString(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(characters.charAt(random.nextInt(characters.length())));
        }
        return sb.toString();
    }
}
