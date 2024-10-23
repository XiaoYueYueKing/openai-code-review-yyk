package cn.yyk.middleware.sdk;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

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
        System.out.println("测试执行");

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
    }
}
