package com.data.userserver.controller;


import com.data.common.Response;
import com.data.common.exception.UserException;
import com.data.common.minio.utils.MinioUtil;
import com.data.common.utils.RsaUtils;
import com.data.userserver.entity.RegisterVo;
import com.data.userserver.service.UserService;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author User
 * @date 2022/7/14 18:16
 */
@RestController
@RequestMapping("/users")
@ApiModel("用户注册登录登出")
public class UserController {
    @Autowired
    private UserService userServiceImpl;

    private static String AVATARBUKET = "avatar-bk";

    //注册
    @PostMapping("/register")
    @ApiOperation("用户注册")
    public Response register( @RequestBody RegisterVo registerVo){
        try{
            userServiceImpl.register(registerVo);
        } catch (UserException e){
            return Response.error().code(e.getCode()).message(e.getMessage());
        }

        return Response.ok();
    }

    @GetMapping("/generateRSA")
    public String logout2() throws Exception {
        RsaUtils.generateKey("ras_pub.pub","rsa.key","zhang",2048);
        return RsaUtils.getPrivateKey("rsa.key").toString();
    }

    @PostMapping("/uploadAvatar")
    @ApiOperation("上传头像")
    public Response uploadAvatar(@RequestParam("file") MultipartFile file)  {
        System.out.println(file.getSize());
        if (file != null && file.getSize() > 100000){
            return Response.error().message("图片大小不能超过100kb!");
        }
        try{
            String objectName = MinioUtil.upload(AVATARBUKET, file);
            String urlPermanent = MinioUtil.getUrlPermanent(AVATARBUKET, objectName);
            userServiceImpl.updateUserAvatar(urlPermanent);
            return Response.ok().data("url",urlPermanent);
        }catch(UserException e){
            return Response.error().message(e.getMessage());
        }catch(Exception e){
            return Response.error();
        }
    }
}
