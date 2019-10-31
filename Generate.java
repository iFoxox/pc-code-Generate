package com.lrp.code.generate;

import java.io.*;
import java.util.*;

/**
 * @author 廖山剑
 * create date 2019/10/21
 */
public class Generate {

    /**
     * 代码生成目录
     */
    private static String targetDir = "/opt/orderCode";

    /**
     * 源目录
     */
    private static String sourceDir = "/Users/iFoxox/Desktop/lrp-service-order";

    /**
     * 要生成的api
     */
    private static String apiName = "LrpOrderSpecimenTransportApi";


    public static void main(String[] args) throws Exception {
        String path = "";
        List<String> strings = initFileName();
        List<File> files = searchFiles(new File(sourceDir), strings);
        for (File file : files) {
            String name = file.getName();
            if (Objects.equals(apiName, name.replaceAll(".java", ""))) {
                path = file.getPath();
            }
            String s = readString(file);
            Set<String> keys = map.keySet();
            for (String key : keys) {
                s = s.replaceAll(key.split(".java")[0], map.get(key).split(".java")[0]);
            }

            writerTarget(map.get(name), s);
        }

        List<String> apiContents = readFile02(path);

        //adapter的代码
        String adapter = generateAdapter(apiContents);
        //service的代码
        String service = generateService(apiContents);
        //controller的代码
        String controller = generateController(apiContents);

        writerTarget(apiName.replaceAll("Api", "AppAdapter.java"), adapter);
        writerTarget(apiName.replaceAll("Api", "AppService.java"), service);
        writerTarget(apiName.replaceAll("Api", "AppApiController.java"), controller);
    }

    private static String generateController(List<String> list) {
        StringBuilder code = new StringBuilder();

        String iName = apiName;
        String serviceName = iName.replaceAll("Api", "AppService");
        String lowerFirst = lowerFirst(serviceName);
        boolean tag = false;
        for (String s : list) {
            if (s.startsWith("@Api(")
                    || s.startsWith("@")
                    || s.startsWith("//")
                    || s.startsWith("/**")
                    || s.startsWith("*")) {
                continue;
            }
            if (s.startsWith("import") && !tag) {
                code.append("import org.springframework.beans.factory.annotation.Autowired;\n");
                code.append("import org.springframework.stereotype.RestController;\n");
                code.append("import lombok.extern.slf4j.Slf4j;\n");
                tag = true;
            }
            if (s.startsWith("public interface")) {
                String i = "AppApiController implements " + iName.replaceAll("Api", "AppApi");
                code.append("@RestController").append("\n");
                code.append("@Slf4j").append("\n");
                code.append(s.replaceAll("interface", "class")
                        .replaceAll(iName, iName.replaceAll("Api", i))).append("\n");
                code.append("").append("\n");
                code.append("    @Autowired").append("\n");
                code.append("    private ").append(serviceName).append(" ").append(lowerFirst).append(";\n");
                code.append("").append("\n");
                continue;
            }
            if (!s.startsWith("ReturnDTO<")) {
                s = s.replaceAll("RequestDTO", "RequestAppDTO").replaceAll("ResponseDTO", "ResponseAppDTO");
            } else if (s.startsWith("ReturnDTO<")) {
                s = s.replaceAll("RequestDTO", "RequestAppDTO").replaceAll("ResponseDTO", "ResponseAppDTO").replaceAll("\\);", "){\n");
                code.append("    @Override\n");
                code.append("    public ").append(s);
                s = s.replaceAll("@RequestBody", "")
                        .replaceAll("@Valid", "")
                        .replaceAll("@RequestParam\\((.*)\"\\)", "")
                        .replaceAll("@PathVariable\\((.*)\"\\)", "");
                String[] split = s.split("\\s+");
                StringBuilder p = new StringBuilder();
                String m = split[1].split("\\(")[0];
                for (String s1 : split) {
                    if (s1.endsWith(",")) {
                        p.append(s1).append(" ");
                    }
                    if (s1.endsWith("){")) {
                        p.append(s1.replaceAll("\\{", ");\n"));
                    }
                }
                code.append("        return Responses.ok(")
                        .append(lowerFirst)
                        .append(".")
                        .append(m).append("(").append(p.toString())
                        .append("    }\n");
                continue;
            }


            code.append(s).append("\n");
        }

//        System.out.println(code.toString());
        return code.toString().replaceAll("PageResponseAppDTO","PageResponseDTO");
    }

    private static String generateService(List<String> list) {
        StringBuilder service = new StringBuilder();

        String iName = apiName;
        String adapterName = iName.replaceAll("Api", "AppAdapter");
        String lowerFirst = lowerFirst(adapterName);
        boolean tag = false;
        for (String s : list) {
            if (s.startsWith("@Api(")
                    || s.startsWith("@")
                    || s.startsWith("//")
                    || s.startsWith("/**")
                    || s.endsWith(".Valid;")
                    || s.endsWith(".PathVariable;")
                    || s.endsWith(".RequestBody;")
                    || s.endsWith(".RequestParam;")
                    || s.startsWith("*")) {
                continue;
            }
            if (s.startsWith("import") && !tag) {
                service.append("import org.springframework.beans.factory.annotation.Autowired;\n");
                service.append("import org.springframework.stereotype.Service;\n");
                tag = true;
            }
            if (s.startsWith("public interface")) {
                service.append("@Service").append("\n");
                service.append(s.replaceAll("interface", "class")
                        .replaceAll(iName, iName.replaceAll("Api", "AppService"))).append("\n");
                service.append("").append("\n");
                service.append("    @Autowired").append("\n");
                service.append("    private ").append(adapterName).append(" ").append(lowerFirst).append(";\n");
                service.append("").append("\n");
                continue;
            }
            if (!s.startsWith("ReturnDTO<")) {
                s = s.replaceAll("RequestDTO", "RequestAppDTO").replaceAll("ResponseDTO", "ResponseAppDTO");
            } else if (s.startsWith("ReturnDTO<")) {
                boolean hasRequestBody = s.contains("@RequestBody");
                s = s.replaceAll("RequestDTO", "RequestAppDTO").replaceAll("ResponseDTO", "ResponseAppDTO");
                s = s.replaceAll("@RequestBody", "")
                        .replaceAll("@Valid", "")
                        .replaceAll("@RequestParam\\((.*)\"\\)", "")
                        .replaceAll("@PathVariable\\((.*)\"\\)", "");
                String[] split = s.split("\\s+");
                String m = "";
                String ret = "";
                StringBuilder p = new StringBuilder();
                StringBuilder ps = new StringBuilder();

                for (String s1 : split) {
                    //返回值
                    if (s1.startsWith("ReturnDTO<")) {
                        String s2 = s1.substring(0, s1.length() - 1).replaceAll("ReturnDTO<", "");
                        ret = s2;
                        service.append("    public ").append(s2).append(" ");
                        continue;
                    }

                    //方法名
                    if (s1.endsWith("(")) {
                        service.append(s1);
                        m = s1;
                        continue;
                    }
                    boolean isList = false;
                    boolean isOne = false;
                    //方法结束
                    if (s1.endsWith(");")) {
                        service.append(ps);
                        service.append(s1.replaceAll(";", "{\n"));
                        if (hasRequestBody && ps.toString().contains("RequestAppDTO")) {
                            if (ps.toString().startsWith("List<")) {
                                String a = ps.toString().trim();
                                String s2 = a.substring(0, a.length() - 1).replaceAll("List<", "");
                                String s4 = convertRequestList(s1.replaceAll("\\);", ""), s2);
                                service.append(s4);
                                isList = true;
                            }else if(ps.toString().startsWith("ValidListDTO<")) {
                                String a = ps.toString().trim();
                                String s2 = a.substring(0, a.length() - 1).replaceAll("ValidListDTO<", "");
                                String s4 = convertValidRequestList(s1.replaceAll("\\);", ""), s2);
                                service.append(s4);
                                isList = true;
                            }
                            else {
                                String a = ps.toString().trim();
                                String s4 = convertRequest(s1.replaceAll("\\);", ""), a);
                                service.append(s4);
                                isOne = true;
                            }
                        }

                        String param = p.toString() + s1.replaceAll(";", "").replaceAll("\\)", "");
                        if (isList) {
                            param = "mapForList";
                        }
                        if (isOne) {
                            param = "requestDTO";
                        }
                        service.append("        ").append(ret.replaceAll("ResponseAppDTO", "ResponseDTO"))
                                .append(" response = ")
                                .append(lowerFirst).append(".")
                                .append(m).append(param)
                                .append(");\n");

                        if (ret.contains("ResponseAppDTO")) {
                            String b = ret.trim();
                            if (ret.contains("Page")) {
                                String s3 = b.substring(0, b.length() - 1).replaceAll("PageResponseDTO<", "");
                                String s4 = convertPageResponse(s3);
                                service.append(s4);
                            } else if (ret.contains("List<")) {
                                String s3 = b.substring(0, b.length() - 1).replaceAll("List<", "");
                                String s4 = convertResponseList(s3);
                                service.append(s4);
                            } else {
                                String s4 = convertResponse(b);
                                service.append(s4);
                            }
                            service.append("        return appResponse;\n");
                            service.append("    }\n");
                        } else {
                            service.append("        return response;\n");
                            service.append("    }\n");
                        }
                    }
                    if (s1.endsWith(",")) {
                        p.append(s1).append(" ");
                    }
                    ps.append(s1).append(" ");

                }
                continue;
            }
            service.append(s).append("\n");
        }
        System.out.println(service.toString());
        return service.toString().replaceAll("PageResponseAppDTO","PageResponseDTO");
    }


    /**
     * 生成Adapter代码
     *
     * @param list
     */
    private static String generateAdapter(List<String> list) {
        StringBuilder adapter = new StringBuilder();

        String interfaceName = apiName;
        String lowerFirst = lowerFirst(interfaceName);
        boolean tag = false;
        for (String s : list) {
            if (s.startsWith("@Api(")
                    || s.startsWith("@")
                    || s.startsWith("//")
                    || s.startsWith("/**")
                    || s.endsWith(".Valid;")
                    || s.endsWith(".PathVariable;")
                    || s.endsWith(".RequestBody;")
                    || s.endsWith(".RequestParam;")
                    || s.startsWith("*")) {
                continue;
            }
            if (s.startsWith("import") && !tag) {
                adapter.append("import com.lrp.utils.ApiParseUtils;\n");
                adapter.append("import org.springframework.beans.factory.annotation.Autowired;\n");
                adapter.append("import org.springframework.stereotype.Component;\n");
                tag = true;
            }
            if (s.startsWith("public interface")) {
                adapter.append("@Component").append("\n");
                adapter.append(s.replaceAll("interface", "class")
                        .replaceAll(interfaceName, interfaceName.replaceAll("Api", "AppAdapter"))).append("\n");
                adapter.append("").append("\n");
                adapter.append("    @Autowired").append("\n");
                adapter.append("    private ").append(interfaceName).append(" ").append(lowerFirst).append(";\n");
                adapter.append("").append("\n");
                continue;
            }
            if (!s.startsWith("ReturnDTO<")) {
//                s = s.replaceAll("RequestDTO", "RequestAppDTO").replaceAll("ResponseDTO", "ResponseAppDTO");
            } else if (s.startsWith("ReturnDTO<")) {
                s = s.replaceAll("@RequestBody", "")
                        .replaceAll("@Valid", "")
                        .replaceAll("@RequestParam\\((.*)\"\\)", "")
                        .replaceAll("@PathVariable\\((.*)\"\\)", "");
                String[] split = s.split("\\s+");
                String m = "";
                StringBuilder p = new StringBuilder();
                StringBuilder ps = new StringBuilder();

                for (String s1 : split) {
                    //返回值
                    if (s1.startsWith("ReturnDTO<")) {
                        String s2 = s1.substring(0, s1.length() - 1).replaceAll("ReturnDTO<", "");
                        adapter.append("    public ").append(s2).append(" ");
                        continue;
                    }

                    //方法名
                    if (s1.endsWith("(")) {
                        adapter.append(s1);
                        m = s1;
                        continue;
                    }
                    //方法结束
                    if (s1.endsWith(");")) {
                        adapter.append(ps);
                        adapter.append(s1.replaceAll(";", "{\n"));
                        adapter.append("        return ApiParseUtils.commonParse(")
                                .append(lowerFirst).append(".")
                                .append(m).append(p.toString())
                                .append(s1.replaceAll(";", "")).append(");\n")
                                .append("    }\n");
                    }
                    if (s1.endsWith(",")) {
                        p.append(s1).append(" ");
                    }
                    ps.append(s1).append(" ");

//                    System.out.println(s1);
                }
                continue;
            }
            adapter.append(s).append("\n");
        }

        return adapter.toString().replaceAll("PageResponseAppDTO","PageResponseDTO");
    }

    public static String lowerFirst(String oldStr) {

        char[] chars = oldStr.toCharArray();

        chars[0] += 32;

        return String.valueOf(chars);
    }


    public static List<String> readFile02(String path) throws IOException {
        // 使用一个字符串集合来存储文本中的路径 ，也可用String []数组
        List<String> list = new ArrayList<String>();
        FileInputStream fis = new FileInputStream(path);
        // 防止路径乱码   如果utf-8 乱码  改GBK     eclipse里创建的txt  用UTF-8，在电脑上自己创建的txt  用GBK
        InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
        BufferedReader br = new BufferedReader(isr);
        String line = "";
        while ((line = br.readLine()) != null) {
            // 如果 t x t文件里的路径 不包含---字符串       这里是对里面的内容进行一个筛选
            list.add(line.trim().replaceAll("import.*.AppConsts","import com.lrp.web.pc.consts.AppConsts;"));
        }
        br.close();
        isr.close();
        fis.close();
        return list;
    }

    /**
     * @param paramName 参数名
     * @param className 类型名
     * @return
     */
    private static String convertRequestList(String paramName, String className) {
        String name = className.replaceAll("RequestAppDTO", "RequestDTO");
        StringBuilder sb = new StringBuilder();
        sb.append("        List<").append(name);
        sb.append("> mapForList = PojoMapperUtils.mapForList(");
        sb.append(paramName).append(", ").append(name).append(".class);\n");
        return sb.toString();
    }

    /**
     * @param paramName 参数名
     * @param className 类型名
     * @return
     */
    private static String convertValidRequestList(String paramName, String className) {
        String name = className.replaceAll("RequestAppDTO", "RequestDTO");
        StringBuilder sb = new StringBuilder();
        sb.append("        ValidListDTO<").append(name);
        sb.append("> mapForList = PojoMapperUtils.mapForValidList(");
        sb.append(paramName).append(", ").append(name).append(".class);\n");
        return sb.toString();
    }

    private static String convertRequest(String paramName, String className) {
        String name = className.replaceAll("RequestAppDTO", "RequestDTO");
        StringBuilder sb = new StringBuilder();
        sb.append("        ").append(name);
        sb.append(" requestDTO = PojoMapperUtils.map(");
        sb.append(paramName).append(", ").append(name).append(".class);\n");
        return sb.toString();
    }

    private static String convertResponseList(String className) {
        StringBuilder sb = new StringBuilder();
        sb.append("        List<").append(className);
        sb.append("> appResponse = PojoMapperUtils.mapForList(");
        sb.append("response").append(", ").append(className).append(".class);\n");
        return sb.toString();
    }

    private static String convertResponse(String className) {
        StringBuilder sb = new StringBuilder();
        sb.append("        ").append(className);
        sb.append(" appResponse = PojoMapperUtils.map(");
        sb.append("response").append(", ").append(className).append(".class);\n");
        return sb.toString();
    }

    private static String convertPageResponse(String className) {
        StringBuilder sb = new StringBuilder();
        sb.append("        ").append(className);
        sb.append(" appResponse = PojoMapperUtils.mapForPageDTO(");
        sb.append("response").append(", ").append(className).append(".class);\n");
        return sb.toString();
    }

    private static String readString(File file) {
        String str = "";
        try {
            FileInputStream in = new FileInputStream(file);
            // size 为字串的长度 ，这里一次性读完
            int size = in.available();
            byte[] buffer = new byte[size];
            in.read(buffer);
            in.close();
            str = new String(buffer);
        } catch (IOException e) {
            return null;
        }
        return str;
    }

    private static Map<String, String> map = new HashMap<>();

    private static List<String> initFileName() {
        String suffix = ".java";
        List<String> list = new ArrayList<>();
        String model = apiName.replace("Api", "");
        list.add(apiName + suffix);
        list.add(model + "AddRequestDTO" + suffix);
        list.add(model + "PageListRequestDTO" + suffix);
        list.add(model + "RequestDTO" + suffix);
        list.add(model + "ResponseDTO" + suffix);
        list.add(model + "UpdateRequestDTO" + suffix);
        list.add(model + "ListRequestDTO" + suffix);
        map.put(apiName + suffix, apiName.replace("Api", "AppApi") + suffix);
        map.put(model + "AddRequestDTO" + suffix, model + "AddRequestAppDTO" + suffix);
        map.put(model + "PageListRequestDTO" + suffix, model + "PageListRequestAppDTO" + suffix);
        map.put(model + "ListRequestDTO" + suffix, model + "ListRequestAppDTO" + suffix);
        map.put(model + "RequestDTO" + suffix, model + "RequestAppDTO" + suffix);
        map.put(model + "ResponseDTO" + suffix, model + "ResponseAppDTO" + suffix);
        map.put(model + "UpdateRequestDTO" + suffix, model + "UpdateRequestAppDTO" + suffix);

        return list;
    }

    private static void writerTarget(String fileName, String content) {
        FileWriter writer;
        try {
            writer = new FileWriter(targetDir + "/" + fileName);
            writer.write(content.replaceAll("import.*.AppConsts","import com.lrp.web.pc.consts.AppConsts;"));
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static List<File> searchFiles(File folder, List<String> fileNames) {
        List<File> result = new ArrayList<>();
        if (folder.isFile()) {
            result.add(folder);
        }
        File[] subFolders = folder.listFiles(file -> {
            if (file.isDirectory()) {
                return true;
            }
            return fileNames.contains(file.getName());
        });

        if (subFolders != null) {
            for (File file : subFolders) {
                if (file.isFile()) {
                    // 如果是文件则将文件添加到结果列表中
                    result.add(file);
                } else {
                    // 如果是文件夹，则递归调用本方法，然后把所有的文件加到结果列表中
                    result.addAll(searchFiles(file, fileNames));
                }
            }
        }
        return result;
    }
}
