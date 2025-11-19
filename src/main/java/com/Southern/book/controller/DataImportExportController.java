package com.Southern.book.controller;

import com.Southern.book.service.DataImportExportService;
import com.opencsv.CSVWriter;
import com.Southern.book.entity.Book;
import com.Southern.book.entity.User;
import com.Southern.book.entity.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/data")
@PreAuthorize("hasRole('ADMIN')")
public class DataImportExportController {

    @Autowired
    private DataImportExportService dataImportExportService;

    @GetMapping("/import-export")
    public String showImportExportPage() {
        return "data/import_export";
    }

    @PostMapping("/import/books")
    public String importBooks(@RequestParam("file") MultipartFile file, Model model) {
        try {
            int count = dataImportExportService.importBooksFromCsv(file);
            model.addAttribute("message", "成功导入 " + count + " 本图书");
        } catch (Exception e) {
            model.addAttribute("error", "导入失败: " + e.getMessage());
        }
        return "data/import_export";
    }
    
    @GetMapping("/download-template")
    public void downloadImportTemplate(HttpServletResponse response) throws IOException {
        // 设置响应头 - 使用CSV格式，与导入功能一致
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=book_import_template.csv");
        
        // 设置UTF-8编码并添加BOM，确保中文正常显示
        response.setCharacterEncoding("UTF-8");
        try (OutputStreamWriter writer = new OutputStreamWriter(response.getOutputStream(), "UTF-8")) {
            // 写入UTF-8 BOM标记
            writer.write("\uFEFF");
            
            // 创建CSVWriter
            try (CSVWriter csvWriter = new CSVWriter(writer)) {
                // 写入表头
                String[] header = {"书名", "作者", "出版社", "出版日期", "ISBN", "分类", "总册数", "可借册数", "价格", "描述"};
                csvWriter.writeNext(header);
                
                // 写入示例数据行
                // ISBN前添加等号前缀，Excel会将其识别为文本格式，避免转换为科学计数法
                String[] exampleRow = {"Java编程思想", "Bruce Eckel", "机械工业出版社", "2019-01-01", "9787111617247", "计算机科学", "5", "5", "139.00", "Java经典教材"};
                csvWriter.writeNext(exampleRow);
            }
        }
    }

    @PostMapping("/import/users")
    public String importUsers(@RequestParam("file") MultipartFile file, Model model) {
        try {
            int count = dataImportExportService.importUsersFromCsv(file);
            model.addAttribute("message", "成功导入 " + count + " 个用户");
        } catch (Exception e) {
            model.addAttribute("error", "导入失败: " + e.getMessage());
        }
        return "data/import_export";
    }
    
    @GetMapping("/export/users")
    public void exportUsers(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=users.csv");
        dataImportExportService.exportUsersToCsv();
    }
    
    @PostMapping("/export")
    public void exportData(@RequestParam("exportType") String exportType, 
                         @RequestParam("format") String format,
                         @RequestParam(value = "includeInactive", defaultValue = "false") boolean includeInactive,
                         HttpServletResponse response) throws IOException {
        
        // 设置响应格式 - 只支持CSV格式
        if (format.equals("CSV")) {
            response.setContentType("text/csv");
            
            // 根据导出类型处理
            switch (exportType) {
                case "BOOKS":
                String filename = "books.csv";
                response.setHeader("Content-Disposition", "attachment; filename=" + filename);
                // 设置UTF-8编码
                response.setCharacterEncoding("UTF-8");
                
                // 使用CSVWriter直接写入响应输出流，并指定UTF-8编码
                try (OutputStreamWriter osw = new OutputStreamWriter(response.getOutputStream(), "UTF-8");
                     CSVWriter writer = new CSVWriter(osw)) {
                    // 添加BOM以确保Excel正确识别中文字符
                    osw.write("\uFEFF");
                        // 写入表头
                        String[] header = {"ID", "ISBN", "Title", "Author", "Publisher", "PublishDate", "Price", "TotalCopies", "AvailableCopies"};
                        writer.writeNext(header);

                        // 获取图书数据并写入
                        List<Book> books = dataImportExportService.getAllBooks();
                        DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                        for (Book book : books) {
                            String[] data = {
                                book.getId().toString(),
                                book.getIsbn() != null ? book.getIsbn() : "",
                                book.getTitle(),
                                book.getAuthor() != null ? book.getAuthor() : "",
                                book.getPublisher() != null ? book.getPublisher() : "",
                                book.getPublishDate() != null ? book.getPublishDate().format(DATE_FORMATTER) : "",
                                book.getPrice() != null ? book.getPrice().toString() : "0",
                                book.getTotalCopies().toString(),
                                book.getAvailableCopies().toString()
                            };
                            writer.writeNext(data);
                        }
                    }
                    break;
                case "USERS":
                filename = "users.csv";
                response.setHeader("Content-Disposition", "attachment; filename=" + filename);
                // 设置UTF-8编码
                response.setCharacterEncoding("UTF-8");
                
                // 使用CSVWriter直接写入响应输出流，并指定UTF-8编码
                try (OutputStreamWriter osw = new OutputStreamWriter(response.getOutputStream(), "UTF-8");
                     CSVWriter writer = new CSVWriter(osw)) {
                    // 添加BOM以确保Excel正确识别中文字符
                    osw.write("\uFEFF");
                        // 写入用户表头
                        String[] header = {"ID", "Username", "Email", "FullName", "Roles", "Enabled"};
                        writer.writeNext(header);

                        // 获取用户数据并写入
                        List<User> users = dataImportExportService.getAllUsers();
                        for (User user : users) {
                            if (!includeInactive && !user.isEnabled()) {
                                continue; // 跳过非活动用户
                            }
                            String[] data = {
                                user.getId().toString(),
                                user.getUsername(),
                                user.getEmail() != null ? user.getEmail() : "",
                                user.getFullName() != null ? user.getFullName() : "",
                                user.getRoles().stream().map(Role::getName).collect(Collectors.joining(",")),
                                String.valueOf(user.isEnabled())
                            };
                            writer.writeNext(data);
                        }
                    }
                    break;
                case "BORROW_RECORDS":
                filename = "borrow_records.csv";
                response.setHeader("Content-Disposition", "attachment; filename=" + filename);
                // 设置UTF-8编码
                response.setCharacterEncoding("UTF-8");
                
                // 使用CSVWriter直接写入响应输出流，并指定UTF-8编码
                try (OutputStreamWriter osw = new OutputStreamWriter(response.getOutputStream(), "UTF-8");
                     CSVWriter writer = new CSVWriter(osw)) {
                    // 添加BOM以确保Excel正确识别中文字符
                    osw.write("\uFEFF");
                        // 写入表头
                        String[] header = {"ID", "User", "Book", "BorrowDate", "DueDate", "ReturnDate", "Status", "FineAmount"};
                        writer.writeNext(header);
                        
                        // 这里需要注入BorrowService并获取借阅记录数据
                        // 暂时返回基本信息
                        writer.writeNext(new String[]{"1", "示例用户", "示例图书", "2024-01-01", "2024-01-15", "2024-01-14", "RETURNED", "0.0"});
                    }
                    break;
                default:
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "无效的导出类型");
            }
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "仅支持CSV格式导出");
        }

    }
/*
    @GetMapping("/export/borrow-records")
    public void exportBorrowRecords(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=borrow_records.csv");
        dataImportExportService.exportBorrowRecordsToCSV(response.getWriter());
    }*/
}