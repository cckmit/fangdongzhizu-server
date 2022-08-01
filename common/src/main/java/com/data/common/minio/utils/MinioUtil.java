package com.data.common.minio.utils;

import cn.hutool.core.lang.ObjectId;import com.data.common.minio.config.MinioConfig;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.Bucket;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import lombok.SneakyThrows;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
public class MinioUtil {
    public static Log log = LogFactory.getLog(MinioUtil.class);

    @Autowired
    MinioConfig minioConfig;
    private static MinioClient minioClient;

    private static String endponint;

    /**
     * 初始化minio配置
     *
     * @param :
     * @return: void
     * @date :
     */

    @PostConstruct
    public void init() {
        try {
            minioClient =
                    MinioClient.builder()
                            .endpoint(minioConfig.getEndpoint())
                            .credentials(minioConfig.getAccessKey(), minioConfig.getSecretKey())
                            .build();
            endponint = minioConfig.getEndpoint();
        } catch (Exception e) {
            e.printStackTrace();
            log.error("minioncliet init error: ", e.fillInStackTrace());
        }
    }

    /**
     * 判断 bucket是否存在
     *
     * @param bucketName: buket名称
     * @return: boolean
     * @date :
     */
    @SneakyThrows(Exception.class)
    public static boolean bucketExists(String bucketName) {
        return minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
    }

    /**
     * 创建 bucket
     *
     * @param bucketName: buket名称
     * @return: void
     * @date :
     */
    @SneakyThrows(Exception.class)
    public static void createBucket(String bucketName) {
        boolean isExist = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!isExist) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(bucketName)
                    .build());
        }
    }

    /**
     * 获取全部bucket
     *
     * @param :
     * @return: java.util.List<io.minio.messages.Bucket>
     * @date :
     */
    @SneakyThrows(Exception.class)
    public static List<Bucket> getAllBuckets() {
        return minioClient.listBuckets();
    }


    /**
     * 将给定流作为对象上传到bucket中
     *
     * @param bucketName:  buket名称
     * @param objectName:  文件名
     * @param inputStream: 文件流
     * @return: java.lang.String : 文件url地址
     * @date :
     */
    @SneakyThrows(Exception.class)
    public static String uploadStream(String bucketName, String objectName, InputStream inputStream, String contentType) {
        ObjectWriteResponse response = minioClient.putObject(PutObjectArgs.builder().bucket(bucketName).object(objectName).stream(
                inputStream, inputStream.available(), -1)
                .contentType(contentType)
                .build());
        return getPresignedObjectUrl(bucketName, objectName);
    }

    /**
     * 文件上传
     *
     * @param bucketName: buket名称
     * @param objectName: 文件
     * @param fileName:   文件路径
     * @date :
     */
    @SneakyThrows(Exception.class)
    public static String uploadFile(String bucketName, String objectName, String fileName) {
        ObjectWriteResponse objectWriteResponse = minioClient.uploadObject(UploadObjectArgs.builder()
                .bucket(bucketName).object(objectName).filename(fileName).build());
        return getPresignedObjectUrl(bucketName, objectName);
    }

    /**
     * 文件上传
     *
     * @param bucketName    buket名称
     * @param multipartFile
     */
    @SneakyThrows
    public static String upload(String bucketName, MultipartFile multipartFile) {

        createBucket(bucketName);
        //随机名称
        String objectName = ObjectId.next();
        // 文件名称
        String originalFilename = objectName + multipartFile.getOriginalFilename();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(multipartFile.getBytes());
        minioClient.putObject(PutObjectArgs.builder().bucket(bucketName)
                .object(originalFilename).stream(inputStream,
                        inputStream.available(), -1)
                .contentType(FileTypeUtils.getFileType(multipartFile))
                .build());

//        return getPresignedObjectUrl(bucketName, originalFilename);
        return originalFilename;

    }

    /**
     * @param bucketName
     * @param fileName
     * @return
     */
    @SneakyThrows
    public static String getUrlPermanent(String bucketName, String fileName) {


        return endponint + "/" + bucketName + "/" + fileName;
    }

    /**
     * 上传视频
     *
     * @param bucketName
     * @param objectName
     * @param fileName
     * @return
     */
    @SneakyThrows(Exception.class)
    public static void uploadVideo(String bucketName, String objectName, String fileName, String contentType) {
        minioClient.uploadObject(UploadObjectArgs.builder()
                .bucket(bucketName).object(objectName).filename(fileName).contentType(contentType).build());
    }


   /* *//**
     * 上传多个对象
     *
     * @param jsonArray
     * @param bucketName
     *//*
    @SneakyThrows(Exception.class)
    public static void uploadMore(JSONArray jsonArray, String bucketName) {
        List<SnowballObject> objects = new ArrayList<SnowballObject>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            objects.add(
                    new SnowballObject(
                            jsonObject.getString("objectName"),
                            jsonObject.getString("fileName")));
            objects.add(
                    new SnowballObject(
                            jsonObject.getString("objectName"),
                            new ByteArrayInputStream(jsonObject.getString("content").getBytes(StandardCharsets.UTF_8)),
                            jsonObject.getString("content").length(),
                            null));
        }

        minioClient.uploadSnowballObjects(
                UploadSnowballObjectsArgs.builder().bucket(bucketName).objects(objects).build());
    }*/

    /**
     * 删除文件
     *
     * @param bucketName: buket名称
     * @param objectName: 对象名称
     * @return: void
     * @date :
     */
    @SneakyThrows(Exception.class)
    public static void deleteFile(String bucketName, String objectName) {
        minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
    }

    /**
     * 批量删除
     * 删除指定桶的多个文件对象,返回删除错误的对象列表，全部删除成功，返回空列表
     *
     * @param objectNames
     * @param bucketName
     */
    @SneakyThrows(Exception.class)
    public static List<String> deleteFileBatch(List<String> objectNames, String bucketName) {
        List<DeleteObject> objects = new LinkedList<>();
        for (String objName : objectNames) {
            objects.add(new DeleteObject(objName));
        }

        Iterable<Result<DeleteError>> results =
                minioClient.removeObjects(
                        RemoveObjectsArgs.builder().bucket(bucketName).objects(objects).build());

        ArrayList<String> resultList = new ArrayList<>();
        for (Result<DeleteError> result : results) {
            DeleteError error = result.get();
            resultList.add(error.objectName());
        }
        return resultList;
    }

    /**
     * 获取minio文件的访问地址
     *
     * @param bucketName: buket名称
     * @param objectName: 对象名称
     * @return: java.lang.String
     * @date :
     */
    @SneakyThrows(Exception.class)
    public static String getPresignedObjectUrl(String bucketName, String objectName) {
        /*Map<String, String> reqParams = new HashMap<String, String>();
        reqParams.put("response-content-type", "application/json");*/

        String url =
                minioClient.getPresignedObjectUrl(
                        GetPresignedObjectUrlArgs.builder()
                                .method(Method.GET)
                                .bucket(bucketName)
                                .object(objectName)
                                .expiry(7, TimeUnit.DAYS)
                                .build());

        return url;
    }

    /**
     * 获取对象的信息和元数据。
     *
     * @param bucketName
     * @param objectName
     */
    @SneakyThrows(Exception.class)
    public static void statObject(String bucketName, String objectName) {

        StatObjectResponse statObjectResponse = minioClient.statObject(
                StatObjectArgs.builder().bucket(bucketName).object(objectName).build());
        System.out.println(statObjectResponse.toString());
    }

    /**
     * 以流的形式获取一个文件对象
     *
     * @param bucketName 存储桶名称
     * @param objectName 存储桶里的对象名称
     * @return
     */
    @SneakyThrows
    public InputStream getObject(String bucketName, String objectName) {

        if (bucketExists(bucketName)) {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build());

        }
        return null;
    }

    /**
     * 以流的形式获取一个文件对象（断点下载）
     *
     * @param bucketName 存储桶名称
     * @param objectName 存储桶里的对象名称
     * @param offset     起始字节的位置
     * @param length     要读取的长度 (可选，如果无值则代表读到文件结尾)
     * @return
     */
    @SneakyThrows
    public InputStream getObject(String bucketName, String objectName, long offset, Long length) {

        if (bucketExists(bucketName)) {

            return minioClient.getObject(GetObjectArgs.builder().bucket(bucketName).object(objectName).offset(offset).length(length).build());
        }
        return null;

    }

    /**
     * 上传文件时，压缩比较大的图片
     *
     * @param buketName
     * @param file
     * @return
     */
    public static Map uploadFile(String buketName, MultipartFile file) {
        HashMap res = new HashMap();
        OutputStream outputStream = new ByteArrayOutputStream();
        ByteArrayInputStream inputStream = null;
        try {
            // 判断上传文件是否为空
            if (null == file || 0 == file.getSize()) {
                res.put("msg", "上传文件不能为空");
                return res;
            }
            /**
             * 判断是否是图片
             * 判断是否超过了 100K
             */
            String fileType = FileTypeUtils.getFileType(file);
            if (fileType.startsWith("image") && (1024 * 1024 * 0.1) <= file.getSize()) {
                // 小于 1M 的
                if ((1024 * 1024 * 0.1) <= file.getSize() && file.getSize() <= (1024 * 1024)) {
                    Thumbnails.of(file.getInputStream()).scale(1f).outputQuality(0.3f).toOutputStream(outputStream);
                }
                // 1 - 2M 的
                else if ((1024 * 1024) < file.getSize() && file.getSize() <= (1024 * 1024 * 2)) {
                }
                // 2M 以上的
                else if ((1024 * 1024 * 2) < file.getSize()) {
                    Thumbnails.of(file.getInputStream()).scale(1f).outputQuality(0.3f).toOutputStream(outputStream);
                }
                byte[] bytes = new byte[((ByteArrayOutputStream) outputStream).size()];
                inputStream = new ByteArrayInputStream(bytes);
                // 开始上传
                String objectName = UUID.randomUUID() + file.getOriginalFilename();
                MinioUtil.uploadStream(buketName, objectName, inputStream, fileType);
                // 返回状态以及图片路径
                res.put("thumbnailUrl", MinioUtil.getUrlPermanent(buketName, objectName));
            } else {
                res.put("thumbnailUrl", null);
            }

            //正常上传
            String objectName = MinioUtil.upload(buketName, file);
            res.put("originUrl", MinioUtil.getUrlPermanent(buketName, objectName));
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            res.put("message", "保存失败！" + e.getMessage());
            return res;
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
