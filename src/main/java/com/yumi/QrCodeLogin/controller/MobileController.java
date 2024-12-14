package com.yumi.QrCodeLogin.controller;

import com.alibaba.fastjson2.JSON;
import com.yumi.QrCodeLogin.respository.CodeStateRepository;
import com.yumi.QrCodeLogin.controller.vo.CodeInfoVo;
import com.yumi.QrCodeLogin.controller.vo.LoginInfoVo;
import com.yumi.QrCodeLogin.respository.TokenKvRepository;
import com.yumi.QrCodeLogin.respository.UserInfoRepository;
import com.yumi.QrCodeLogin.respository.dos.LoginType;
import com.yumi.QrCodeLogin.respository.dos.TokenDo;
import com.yumi.QrCodeLogin.respository.dos.UserDo;
import jakarta.annotation.Resource;
import jakarta.websocket.Session;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.UUID;

@RequestMapping("/mobile")
@Controller
public class MobileController {

    @Resource
    private UserInfoRepository userInfoRepository;
    @Resource
    private TokenKvRepository tokenKvRepository;
    @Resource
    private CodeStateRepository codeStateRepository;

    @GetMapping("/login")
    @ResponseBody
    public LoginInfoVo login() {
        UserDo user = userInfoRepository.getUser();
        TokenDo tokenDo = new TokenDo();
        tokenDo.setDeadline(System.currentTimeMillis() + 7 * 86400 * 1000);
        tokenDo.setLoginType(LoginType.MOBILE);
        tokenDo.setUserName(user.getUsername());
        String token = UUID.randomUUID().toString();
        LoginInfoVo mobileLoginInfoVo = new LoginInfoVo();
        mobileLoginInfoVo.setToken(token);
        mobileLoginInfoVo.setUsername(user.getUsername());
        tokenKvRepository.save(token, tokenDo);
        return mobileLoginInfoVo;
    }

    @GetMapping("/page")
    public String page() {
        return "mobilePage";
    }

    /**
     * 手机端扫描授权pc登录
     *
     * @param code：                     二维码 code
     * @param username：用户名
     * @param token：用户token（登录用户的token）
     * @return
     * @throws IOException
     */
    @GetMapping("/scan")
    @ResponseBody
    public CodeInfoVo scan(@RequestParam("code") String code,
        @RequestParam("username") String username,
        @RequestParam("token") String token) throws IOException {
        TokenDo tokenDo = tokenKvRepository.get(token);
        if (tokenDo.getDeadline() < System.currentTimeMillis()) {
            throw new IllegalArgumentException("token 失效");
        }
        if (!username.equals(tokenDo.getUserName()) || !LoginType.MOBILE.equals(tokenDo.getLoginType())) {
            throw new IllegalArgumentException("token 信息有误");
        }
        code = codeStateRepository.getCode(code);

        // 获取二维码对应 websocket 会话信息
        Session session = codeStateRepository.getSession(code);
        if (null == session || !session.isOpen()) {
            throw new IllegalArgumentException("code 已过期");
        }

        String tempCode = UUID.randomUUID().toString();

        // 建立用户与二维码的关系【code - username】
        codeStateRepository.addCode(tempCode, code);
        LoginInfoVo loginInfoVo = new LoginInfoVo();
        loginInfoVo.setUsername(username);

        // 通过websocket 通知前端现在谁在扫描
        session.getBasicRemote().sendText(JSON.toJSONString(loginInfoVo));

        CodeInfoVo codeInfoVo = new CodeInfoVo();
        codeInfoVo.setCode(tempCode);
        return codeInfoVo;
    }

    /**
     * 扫描之后 --> 确认登录
     *
     * @param token    用户token（登录用户的token）
     * @param username 用户名
     * @param code     二维码 code
     * @return
     * @throws IOException
     */
    @GetMapping("/confirm")
    @ResponseBody
    public ResponseEntity<?> confirm(@RequestParam("token") String token,
        @RequestParam("username") String username,
        @RequestParam("code") String code) throws IOException {

        TokenDo tokenDo = tokenKvRepository.get(token);
        if (tokenDo.getDeadline() < System.currentTimeMillis()) {
            throw new IllegalArgumentException("token 失效");
        }
        if (!username.equals(tokenDo.getUserName()) || !LoginType.MOBILE.equals(tokenDo.getLoginType())) {
            throw new IllegalArgumentException("token 信息有误");
        }
        String targetCode = codeStateRepository.getCode(code);
        if (null == targetCode) {
            throw new IllegalArgumentException("code 已过期");
        }
        Session session = codeStateRepository.getSession(targetCode);
        if (null == session || !session.isOpen()) {
            throw new IllegalArgumentException("code 已过期");
        }

        String newToken = UUID.randomUUID().toString();

        TokenDo newTokenDo = new TokenDo();
        newTokenDo.setUserName(tokenDo.getUserName());
        newTokenDo.setDeadline(System.currentTimeMillis() + 7 * 86400 * 1000);
        newTokenDo.setLoginType(LoginType.PC);

        // 保存 扫描登录 新登录token 信息
        tokenKvRepository.save(newToken, newTokenDo);

        LoginInfoVo loginInfoVo = new LoginInfoVo();
        loginInfoVo.setUsername(username);
        loginInfoVo.setToken(newToken);

        // websocket 发送消息确认登录
        session.getBasicRemote().sendText(JSON.toJSONString(loginInfoVo));

        return ResponseEntity.ok().build();
    }
}
