package cn.yyk.middleware.sdk;

import cn.yyk.middleware.sdk.domain.model.Model;
import cn.yyk.middleware.sdk.domain.service.impl.OpenAiCodeReviewService;
import cn.yyk.middleware.sdk.infrastructure.git.GitCommand;
import cn.yyk.middleware.sdk.infrastructure.openai.IOpenAI;
import cn.yyk.middleware.sdk.infrastructure.openai.impl.ChatGLM;
import cn.yyk.middleware.sdk.infrastructure.weixin.WeiXin;
import cn.yyk.middleware.sdk.types.utils.BearerTokenUtils;
import cn.yyk.middleware.sdk.types.utils.WXAccessTokenUtils;
import com.alibaba.fastjson2.JSON;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger logger = LoggerFactory.getLogger(OpenAiCodeReview.class);

    // 微信配置
    private String weixin_appid = "wx377aa343b594c7eb";
    private String weixin_secret = "dc06416ee854bf2a84a1e7231c764e6a";
    private String weixin_touser = "o-dcR63T9h__StN5ofDFbXM4wiNE";
    private String weixin_template_id = "hEFhtKRGH6kotEyeVkVgnNwTjlZJoTAePa2BE3oBWfQ";

    // ChatGLM 配置
    private String chatglm_apiHost = "https://open.bigmodel.cn/api/paas/v4/chat/completions";
    private String chatglm_apiKeySecret = "";

    // Github 配置
    private String github_review_log_uri;
    private String github_token;

    // 工程配置 - 自动获取
    private String github_project;
    private String github_branch;
    private String github_author;

    public static void main(String[] args) throws Exception {
        System.out.println("openai 代码评审，测试执行");

        GitCommand gitCommand = new GitCommand(
                getEnv("GITHUB_REVIEW_LOG_URI"),
                getEnv("GITHUB_TOKEN"),
                getEnv("COMMIT_PROJECT"),
                getEnv("COMMIT_BRANCH"),
                getEnv("COMMIT_AUTHOR"),
                getEnv("COMMIT_MESSAGE")
        );
        //微信的四个参数
        /**
         * 项目：{{repo_name.DATA}} 分支：{{branch_name.DATA}} 作者：{{commit_author.DATA}} 说明：{{commit_message.DATA}}
         */
        WeiXin weiXin = new WeiXin(
                getEnv("WEIXIN_APPID"),
                getEnv("WEIXIN_SECRET"),
                getEnv("WEIXIN_TOUSER"),
                getEnv("WEIXIN_TEMPLATE_ID")
        );


        IOpenAI openAI = new ChatGLM(getEnv("CHATGLM_APIHOST"), getEnv("CHATGLM_APIKEYSECRET"));

        OpenAiCodeReviewService openAiCodeReviewService = new OpenAiCodeReviewService(gitCommand, openAI, weiXin);
        openAiCodeReviewService.exec();

        logger.info("openai-code-review done!");
    }

    private static String getEnv(String key) {
        String value = System.getenv(key);
        if (null == value || value.isEmpty()) {
            throw new RuntimeException("value is null");
        }
        return value;
    }
}
