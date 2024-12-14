package com.yumi.QrCodeLogin.controller;

import com.yumi.QrCodeLogin.respository.CodeStateRepository;
import com.yumi.QrCodeLogin.util.QRCodeGenerator;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

@RequestMapping("/pc")
@Controller
public class PcController {

    @Value("${server.port:8080}")
    private int port;

    @Resource
    private CodeStateRepository codeStateRepository;


    @GetMapping("/qrLogin")
    public String loginPage() {
        return "rqLogin";
    }

    /**
     * pc端登录二维码
     *
     * @return
     */
    @GetMapping("/qrCode")
    public ResponseEntity<byte[]> qrCode( HttpServletResponse response) {
        String code = UUID.randomUUID().toString();
        // 保存二维码 code 信息
//        String url = "http://localhost:" + port + "/mobile/scan" + "?code=" + code;
        codeStateRepository.addCode(code);
        System.out.println("qrCode=" + code);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(QRCodeGenerator.generate(code));
    }
}
