package com.data.common;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * @author User
 * @date 2022/7/14 14:28
 */
@Data
public class Response {

    @ApiModelProperty("是否成功")
    private boolean success;

    @ApiModelProperty("响应码")
    private Integer code;

    @ApiModelProperty("返回信息")
    private String message;

    @ApiModelProperty("返回数据")
    private Map<String, Object> data = new HashMap<String, Object>();

    private Response() {
    }

    public static Response ok(){
        Response r = new Response();
        r.setSuccess(true);
        r.setCode(20001);
        r.setMessage("成功!");
        return r;
    }

    //失败 静态方法
    public static Response error(){
        Response r = new Response();
        r.setSuccess(false);
        r.setCode(20002);
        r.setMessage("失败!");
        return r;
    }

    public Response success(Boolean success){
        this.setSuccess(success);
        return this;
    }

    public Response code(Integer code){
        this.setCode(code);
        return this;
    }

    public Response message(String message){
        this.setMessage(message);
        return this;
    }

    public Response data(String key, Object value){
        this.data.put(key,value);
        return this;
    }

    public Response data(Map<String, Object> map){
        this.setData(map);
        return this;
    }

}
