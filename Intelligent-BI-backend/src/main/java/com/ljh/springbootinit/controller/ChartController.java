package com.ljh.springbootinit.controller;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.TypeReference;
import cn.hutool.json.JSONUtil;
import cn.hutool.json.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.ljh.springbootinit.bizmq.BiMessageProducer;
import com.ljh.springbootinit.mapper.ChartMapper;
import com.ljh.springbootinit.model.dto.chart.*;
import com.ljh.springbootinit.model.entity.Chart;
import com.ljh.springbootinit.model.entity.User;
import com.ljh.springbootinit.model.enums.ChartStatusEnum;
import com.ljh.springbootinit.model.vo.ChartVO;
import com.ljh.springbootinit.service.ChartService;
import com.ljh.springbootinit.annotation.AuthCheck;
import com.ljh.springbootinit.common.BaseResponse;
import com.ljh.springbootinit.common.DeleteRequest;
import com.ljh.springbootinit.common.ErrorCode;
import com.ljh.springbootinit.common.ResultUtils;
import com.ljh.springbootinit.constant.CommonConstant;
import com.ljh.springbootinit.constant.UserConstant;
import com.ljh.springbootinit.exception.BusinessException;
import com.ljh.springbootinit.exception.ThrowUtils;
import com.ljh.springbootinit.manager.AiManager;
import com.ljh.springbootinit.manager.RedisLimiterManager;
import com.ljh.springbootinit.model.vo.BiResponse;
import com.ljh.springbootinit.service.UserService;
import com.ljh.springbootinit.utils.AIResponseHandler;
import com.ljh.springbootinit.utils.ExcelUtils;
import com.ljh.springbootinit.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 图表接口
 */
@RestController
@RequestMapping("/chart")
@Slf4j
public class ChartController {

    @Resource
    private ChartService chartService;

    @Resource
    private UserService userService;

    @Resource
    private ChartMapper chartMapper;

    @Resource
    private AiManager aiManager;

    @Resource
    private RedisLimiterManager redisLimiterManager;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private BiMessageProducer biMessageProducer;


    // region 增删改查

    /**
     * 创建
     *
     * @param chartAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addChart(@RequestBody ChartAddRequest chartAddRequest, HttpServletRequest request) {
        if (chartAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartAddRequest, chart);
        User loginUser = userService.getLoginUser(request);
        chart.setUserId(loginUser.getId());
        boolean result = chartService.save(chart);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newChartId = chart.getId();
        return ResultUtils.success(newChartId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteChart(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldChart.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = chartService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新（仅管理员）
     *
     * @param chartUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateChart(@RequestBody ChartUpdateRequest chartUpdateRequest) {
        if (chartUpdateRequest == null || chartUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartUpdateRequest, chart);
        long id = chartUpdateRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<Chart> getChartById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = chartService.getById(id);
        if (chart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(chart);
    }

    /**
     * 分页获取列表（封装类）
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<Chart>> listChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
                                                     HttpServletRequest request) {
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page")
    public BaseResponse<Page<Chart>> listMyChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
                                                       HttpServletRequest request) {
        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        chartQueryRequest.setUserId(loginUser.getId());
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    // endregion

    /**
     * 编辑（用户）
     *
     * @param chartEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editChart(@RequestBody ChartEditRequest chartEditRequest, HttpServletRequest request) {
        if (chartEditRequest == null || chartEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartEditRequest, chart);
        User loginUser = userService.getLoginUser(request);
        long id = chartEditRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldChart.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 智能分析（同步）
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen")
    public BaseResponse<BiResponse> genChartByAi(@RequestPart("file") MultipartFile multipartFile,
                                                 GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        String tags = genChartByAiRequest.getTags();

        // 校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");
        // 校验文件
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        // 校验文件大小
        final long ONE_MB = 1024 * 1024L;
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件超过 1M");
        // 校验文件后缀 aaa.png
        String suffix = FileUtil.getSuffix(originalFilename);
        final List<String> validFileSuffixList = Arrays.asList("xlsx");
        ThrowUtils.throwIf(!validFileSuffixList.contains(suffix), ErrorCode.PARAMS_ERROR, "文件后缀非法");

        User loginUser = userService.getLoginUser(request);
        // 限流判断，每个用户一个限流器
        redisLimiterManager.doRateLimit("genChartByAi_" + loginUser.getId());
        // 无需写 prompt，使用鱼聪明AI，直接调用现有模型
//        final String prompt = "你是一个数据分析师和前端开发专家，接下来我会按照以下固定格式给你提供内容：\n" +
//                "分析需求：\n" +
//                "{数据分析的需求或者目标}\n" +
//                "原始数据：\n" +
//                "{csv格式的原始数据，用,作为分隔符}\n" +
//                "请根据这两部分内容，按照以下指定格式生成内容（此外不要输出任何多余的开头、结尾、注释）\n" +
//                "【【【【【\n" +
//                "{前端 Echarts V5 的 option 配置对象js代码，合理地将数据进行可视化，不要生成任何多余的内容，比如注释}\n" +
//                "【【【【【\n" +
//                "{明确的数据分析结论、越详细越好，不要生成多余的注释}";
        long biModelId = 1659171950288818178L;
        // 分析需求：
        // 分析网站用户的增长情况
        // 请使用
        // 饼图
        // 原始数据：
        // 日期,用户数
        // 1号,10
        // 2号,20
        // 3号,30

//        // 鱼聪明 AI
//        // 构造用户输入
//        StringBuilder userInput = new StringBuilder();
//        userInput.append("分析需求：").append("\n");
//
//        // 拼接分析目标
//        String userGoal = goal;
//        if (StringUtils.isNotBlank(chartType)) {
//            userGoal += "，请使用" + chartType;
//        }
//        userInput.append(userGoal).append("\n");
//        userInput.append("原始数据：").append("\n");
//        // 压缩后的数据
//        String csvData = ExcelUtils.excelToCsv(multipartFile);
//        userInput.append(csvData).append("\n");


//        // 星火 AI
        // 构造用户输入
        String basicStr = "请根据以下数据生成一个 ECharts 兼容的 JSON 配置对象，并进行数据分析\n";
        StringBuilder userInput = new StringBuilder();
        userInput.append(basicStr);
        userInput.append("分析需求：").append("\n");

        // 拼接分析目标
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)) {
            userGoal += "，请使用" + chartType;
        }
        userInput.append(userGoal).append("\n");
        userInput.append("原始数据：").append("\n");
        // 压缩后的数据
        String csvData = ExcelUtils.excelToCsv(multipartFile);
        userInput.append(csvData).append("\n");
        userInput.append("请按照以下格式输出：\n" +
                "\n" +
                "text：\n" +
                "{请写下分析结论}\n" +
                "\n" +
                "chart：\n" +
                "```json\n" +
                "{这里是图表的 JSON 配置}");


//        String result = aiManager.doChat(biModelId, userInput.toString());
//        String result = aiManager.sendMsgToXingHuo(true, userInput.toString());


//        String[] splits = result.split("【【【【【");
//        if (splits.length < 3) {
//            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 生成错误");
//        }
//        String genChart = splits[1].trim();
//        String genResult = splits[2].trim();

        //获取响应
        String oriResponse = aiManager.doChatxinghuo(userInput.toString());
        System.out.println(oriResponse);
        //工具类处理返回的响应
        List<String> results = AIResponseHandler.handleResponse(oriResponse);
        String genChart = results.get(0);
        String genResult = results.get(1);

        // 插入到数据库
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setStatus(String.valueOf(ChartStatusEnum.SUCCEED.getValue()));
        // 处理标签
        try {
            String tag= URLDecoder.decode( tags , "UTF-8" ); // 将tags字段解码为UTF-8的字符串
            chart.setTags(tag);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        chart.setGenChart(genChart);
        chart.setGenResult(genResult);
        chart.setUserId(loginUser.getId());
        chart.setUserAvatar(loginUser.getUserAvatar());
        boolean saveResult = chartService.save(chart);
        // 分库分表实现
        Long newId = chart.getId();
        // 提取数据的第一行字段，并创建数据库表
        String[] lines = csvData.split("\n");
        String[] dataLines = Arrays.copyOfRange(lines, 1, lines.length); // 获取除第一行外的所有数据行

        String firstLine = lines[0];
        String[] data = firstLine.split(","); // 获取数据的各个字段
        try {
            String tableName = "chart_" + newId;
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/intelligent_bi", "root", "123456");
            Statement stmt = conn.createStatement();

            StringBuilder query = new StringBuilder("CREATE TABLE " + tableName + "(");
            for (String fieldName : data) {
                query.append(fieldName).append(" VARCHAR(255), "); // 根据实际需求定义字段类型和长度
            }
            query.deleteCharAt(query.length() - 2); // 移除最后一个逗号
            query.append(")");

            stmt.executeUpdate(query.toString());

            System.out.println("Table created successfully.");

            for (String line : dataLines) {
                String[] dataResult = line.split(",");
                String insertQuery = "INSERT INTO " + tableName + " VALUES ('" + dataResult[0] + "', '" + dataResult[1] + "')";

                stmt.executeUpdate(insertQuery);
            }

            System.out.println("Data operation successfully.");

            stmt.close();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");
        BiResponse biResponse = new BiResponse();
        biResponse.setGenChart(genChart);
        biResponse.setGenResult(genResult);
        biResponse.setChartId(chart.getId());
        return ResultUtils.success(biResponse);
    }

    /**
     * 智能分析（异步）
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen/async")
    public BaseResponse<BiResponse> genChartByAiAsync(@RequestPart("file") MultipartFile multipartFile,
                                                 GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        // 校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");
        // 校验文件
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        // 校验文件大小
        final long ONE_MB = 1024 * 1024L;
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件超过 1M");
        // 校验文件后缀
        String suffix = FileUtil.getSuffix(originalFilename);
        final List<String> validFileSuffixList = Arrays.asList("xlsx","xls");
        ThrowUtils.throwIf(validFileSuffixList.contains(suffix), ErrorCode.PARAMS_ERROR, "文件后缀非法");

        User loginUser = userService.getLoginUser(request);
        // 限流判断，每个用户一个限流器
        redisLimiterManager.doRateLimit("genChartByAi_" + loginUser.getId());
        // 无需写 prompt，直接调用现有模型，https://www.yucongming.com，公众号搜【鱼聪明AI】
//        final String prompt = "你是一个数据分析师和前端开发专家，接下来我会按照以下固定格式给你提供内容：\n" +
//                "分析需求：\n" +
//                "{数据分析的需求或者目标}\n" +
//                "原始数据：\n" +
//                "{csv格式的原始数据，用,作为分隔符}\n" +
//                "请根据这两部分内容，按照以下指定格式生成内容（此外不要输出任何多余的开头、结尾、注释）\n" +
//                "【【【【【\n" +
//                "{前端 Echarts V5 的 option 配置对象js代码，合理地将数据进行可视化，不要生成任何多余的内容，比如注释}\n" +
//                "【【【【【\n" +
//                "{明确的数据分析结论、越详细越好，不要生成多余的注释}";
        long biModelId = 1659171950288818178L;
        // 分析需求：
        // 分析网站用户的增长情况
        // 原始数据：
        // 日期,用户数
        // 1号,10
        // 2号,20
        // 3号,30

        // 构造用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：").append("\n");

        // 拼接分析目标
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)) {
            userGoal += "，请使用" + chartType;
        }
        userInput.append(userGoal).append("\n");
        userInput.append("原始数据：").append("\n");
        // 压缩后的数据
        String csvData = ExcelUtils.excelToCsv(multipartFile);
        userInput.append(csvData).append("\n");


        // 插入到数据库
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setStatus(String.valueOf(ChartStatusEnum.WAIT.getValue()));
        chart.setUserId(loginUser.getId());
        chart.setUserAvatar(loginUser.getUserAvatar());
        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");

        // todo 建议处理任务队列满了后，抛异常的情况
        CompletableFuture.runAsync(() -> {
            // 先修改图表任务状态为 “执行中”。等执行成功后，修改为 “已完成”、保存执行结果；执行失败后，状态修改为 “失败”，记录任务失败信息。
            Chart updateChart = new Chart();

            updateChart.setId(chart.getId());
            updateChart.setStatus(String.valueOf(ChartStatusEnum.RUNNING.getValue()));
            boolean b = chartService.updateById(updateChart);
            if (!b) {
                handleChartUpdateError(chart.getId(), "更新图标执行中状态失败");
                return;
            }
            // 调用 Ai
            String result = aiManager.doChat(biModelId, userInput.toString());
            String[] splits = result.split("【【【【【");
            if (splits.length < 3) {
                handleChartUpdateError(chart.getId(), "AI 生成错误");
                return;
            }
            String genChart = splits[1].trim();
            String genResult = splits[2].trim();

            Chart updateChartResult = new Chart();
            updateChartResult.setId(chart.getId());
            updateChartResult.setGenChart(genChart);
            updateChartResult.setGenResult(genResult);

            updateChartResult.setStatus(String.valueOf(ChartStatusEnum.SUCCEED.getValue()));
            boolean updateResult = chartService.updateById(updateChartResult);
            if (!updateResult) {
                handleChartUpdateError(chart.getId(), "更新图表成功状态失败");
            }

            // 分库分表实现
            Long newId = chart.getId();
            // 提取数据的第一行字段，并创建数据库表
            String[] lines = csvData.split("\n");
            String[] dataLines = Arrays.copyOfRange(lines, 1, lines.length); // 获取除第一行外的所有数据行

            String firstLine = lines[0];
            String[] data = firstLine.split(",");
            try {
                String tableName = "chart_" + newId; // 假设 chart_id 是表名的一部分
                Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/intelligent_bi", "root", "123456");
                Statement stmt = conn.createStatement();

                StringBuilder query = new StringBuilder("CREATE TABLE " + tableName + "(");
                for (String fieldName : data) {
                    query.append(fieldName).append(" VARCHAR(255), "); // 根据实际需求定义字段类型和长度
                }
                query.deleteCharAt(query.length() - 2); // 移除最后一个逗号
                query.append(")");

                stmt.executeUpdate(query.toString());

                System.out.println("Table created successfully."+query);

                for (String line : dataLines) {
                    String[] dataResult = line.split(",");
                    String insertQuery = "INSERT INTO " + tableName + " VALUES ('" + dataResult[0] + "', '" + dataResult[1] + "')";

                    stmt.executeUpdate(insertQuery);
                }

                System.out.println("Data operation successfully.");

                stmt.close();
                conn.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, threadPoolExecutor);



        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(chart.getId());
        return ResultUtils.success(biResponse);
    }

    /**
     * 智能分析（异步消息队列）
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen/async/mq")
    public BaseResponse<BiResponse> genChartByAiAsyncMq(@RequestPart("file") MultipartFile multipartFile,
                                                      GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        String tags = genChartByAiRequest.getTags();
        // 校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");
        // 校验文件
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        // 校验文件大小
        final long ONE_MB = 1024 * 1024L;
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件超过 1M");
        // 校验文件后缀
        String suffix = FileUtil.getSuffix(originalFilename);
        final List<String> validFileSuffixList = Arrays.asList("xlsx","xls");
        ThrowUtils.throwIf(!validFileSuffixList.contains(suffix), ErrorCode.PARAMS_ERROR, "文件后缀非法");

        User loginUser = userService.getLoginUser(request);
        // 限流判断，每个用户一个限流器
        redisLimiterManager.doRateLimit("genChartByAi_" + loginUser.getId());
        // 无需写 prompt，直接调用现有模型
//        final String prompt = "你是一个数据分析师和前端开发专家，接下来我会按照以下固定格式给你提供内容：\n" +
//                "分析需求：\n" +
//                "{数据分析的需求或者目标}\n" +
//                "原始数据：\n" +
//                "{csv格式的原始数据，用,作为分隔符}\n" +
//                "请根据这两部分内容，按照以下指定格式生成内容（此外不要输出任何多余的开头、结尾、注释）\n" +
//                "【【【【【\n" +
//                "{前端 Echarts V5 的 option 配置对象js代码，合理地将数据进行可视化，不要生成任何多余的内容，比如注释}\n" +
//                "【【【【【\n" +
//                "{明确的数据分析结论、越详细越好，不要生成多余的注释}";
        long biModelId = CommonConstant.BI_Chart_MODEL_ID;
        // 分析需求：
        // 分析网站用户的增长情况
        // 原始数据：
        // 日期,用户数
        // 1号,10
        // 2号,20
        // 3号,30

        // 构造用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：").append("\n");

        // 拼接分析目标
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)) {
            userGoal += "，请使用" + chartType;
        }
        userInput.append(userGoal).append("\n");
        userInput.append("原始数据：").append("\n");
        // 压缩后的数据
        String csvData = ExcelUtils.excelToCsv(multipartFile);
        userInput.append(csvData).append("\n");

        // 插入到数据库
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setStatus(String.valueOf(ChartStatusEnum.WAIT.getValue()));
        chart.setUserId(loginUser.getId());
        chart.setUserAvatar(loginUser.getUserAvatar());
        try {
            String decodedTags = URLDecoder.decode(tags, "UTF-8");
            System.out.println(decodedTags);
            chart.setTags(decodedTags);
            // 输出示例: ["折线图"]
        } catch (UnsupportedEncodingException e) {
            System.out.println("URL decoding failed: " + e.getMessage());
        }

        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");
        long newChartId = chart.getId();

        // 分库分表实现

        // 提取数据的第一行字段，并创建数据库表
        String[] lines = csvData.split("\n");
        String[] dataLines = Arrays.copyOfRange(lines, 1, lines.length); // 获取除第一行外的所有数据行

        String firstLine = lines[0];
        String[] data = firstLine.split(",");
        try {
            String tableName = "chart_" + newChartId; // 假设 chart_id 是表名的一部分
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/intelligent_bi", "root", "123456");
            Statement stmt = conn.createStatement();

            StringBuilder query = new StringBuilder("CREATE TABLE " + tableName + "(");
            for (String fieldName : data) {
                query.append(fieldName).append(" VARCHAR(255), "); // 根据实际需求定义字段类型和长度
            }
            query.deleteCharAt(query.length() - 2); // 移除最后一个逗号
            query.append(")");

            stmt.executeUpdate(query.toString());

            System.out.println("Table created successfully."+query );

            for (String line : dataLines) {
                String[] dataResult = line.split(",");
                String insertQuery = "INSERT INTO " + tableName + " VALUES ('" + dataResult[0] + "', '" + dataResult[1] + "')";

                stmt.executeUpdate(insertQuery);
            }

            System.out.println("Data operation successfully.");

            stmt.close();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        biMessageProducer.sendMessage(String.valueOf(newChartId));
        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(newChartId);
        return ResultUtils.success(biResponse);
    }

    /**
     * 图表重新生成（mq）
     * @param chartRebuildRequest
     * @param request
     * @return
     */
    @PostMapping("/gen/async/rebuild")
    public BaseResponse<BiResponse> genChartByAiAsyncMq(ChartRebuildRequest chartRebuildRequest, HttpServletRequest request) {
        Long chartId = chartRebuildRequest.getId();
        Chart genChartByAiRequest  = chartService.getById(chartId);
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartData = genChartByAiRequest.getChartData();
        String chartType = genChartByAiRequest.getChartType();
        // 校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");
        ThrowUtils.throwIf(StringUtils.isBlank(chartData),ErrorCode.PARAMS_ERROR,"表格数据为空");
        ThrowUtils.throwIf(StringUtils.isBlank(chartType),ErrorCode.PARAMS_ERROR,"生成表格类型为空");

        User loginUser = userService.getLoginUser(request);
        // 限流判断，每个用户一个限流器
        redisLimiterManager.doRateLimit("genChartByAi_" + loginUser.getId());

        // 插入到数据库
        Chart chart = new Chart();
        chart.setStatus(String.valueOf(ChartStatusEnum.WAIT.getValue()));
        chart.setId(chartId);
        boolean saveResult = chartService.updateById(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");
        log.warn("准备发送信息给队列，Message={}=======================================",chartId);
        biMessageProducer.sendMessage(String.valueOf(chartId));
        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(chart.getId());
        return ResultUtils.success(biResponse);
    }


    /**
     * 获取图表信息
     *
     * @param chartId
     * @param execMessage
     * @return
     */
    @GetMapping("/queryChartData")
    public  BaseResponse<List<Map<String, Object>>> queryChartData (long chartId,String execMessage) {
        if (chartId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String querySql = String.format("select * from chart_%s", chartId);
        List<Map<String, Object>> resultData = chartMapper.queryChartData(querySql);

        return ResultUtils.success(resultData);
    }

    /**
     * 更新图表状态为失败
     * @param chartId
     * @param execMessage
     */
    private void handleChartUpdateError(long chartId,String execMessage) {
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chartId);
        updateChartResult.setStatus(String.valueOf(ChartStatusEnum.FAILED.getValue()));
        updateChartResult.setExecMessage("execMessage");
        boolean updateResult = chartService.updateById(updateChartResult);
        if (!updateResult) {
            log.error("更新图表失败状态失败" + chartId + "," + execMessage);
        }
    }

    /**
     * 根据 id 获取 图表脱敏
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<ChartVO> getChartVOById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = chartService.getById(id);
        if (chart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        ChartVO chartVO = new ChartVO();
        BeanUtils.copyProperties(chart,chartVO);
        return ResultUtils.success(chartVO);
    }

    /**
     * 获取查询包装类
     *
     * @param chartQueryRequest
     * @return
     */
    private QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest) {
        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
        if (chartQueryRequest == null) {
            return queryWrapper;
        }
        Long id = chartQueryRequest.getId();
        String name = chartQueryRequest.getName();
        String goal = chartQueryRequest.getGoal();
        String tag = chartQueryRequest.getTags();
        String chartType = chartQueryRequest.getChartType();
        Long userId = chartQueryRequest.getUserId();
        String userAvatar = chartQueryRequest.getUserAvatar();
//        String sortField = chartQueryRequest.getSortField();
        String sortField ="updateTime";
        String sortOrder = chartQueryRequest.getSortOrder();

        queryWrapper.eq(id != null && id > 0, "id", id);

//        queryWrapper.eq(StringUtils.isNotBlank(goal), "goal", goal);
        queryWrapper.eq(StringUtils.isNotBlank(goal), "chartType", chartType);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StringUtils.isNotBlank(name), "name", name)
                .or()
                .like(StringUtils.isNotBlank(goal), "goal", goal);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    @PostMapping("/test")
    public BaseResponse<Long> test(@RequestBody ChartQueryRequest chartQueryRequest, HttpServletRequest request) {
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartQueryRequest, chart);
        User loginUser = userService.getLoginUser(request);
        String tags = chartQueryRequest.getTags();
        try {
            String decodedTags = URLDecoder.decode(tags, "UTF-8");
            System.out.println(decodedTags);
            chart.setTags(decodedTags);
            // 输出: ["折线图"]
        } catch (UnsupportedEncodingException e) {
            System.out.println("URL decoding failed: " + e.getMessage());
        }
        chart.setUserId(loginUser.getId());
        chart.setStatus(String.valueOf(ChartStatusEnum.SUCCEED.getValue()));


        boolean result = chartService.save(chart);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newChartId = chart.getId();
        return ResultUtils.success(newChartId);
    }

}
