package com.Southern.book.controller;

import com.Southern.book.service.DataImportExportService;
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
import java.io.InputStream;

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
        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=book_import_template.xlsx");
        
        // 在实际项目中，这里应该提供一个真实的模板文件
        // 目前仅返回一个基本的错误提示
        response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED, "模板文件下载功能未实现");
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
        
        // 设置响应格式
        if (format.equals("CSV")) {
            response.setContentType("text/csv");
        } else if (format.equals("EXCEL")) {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        }
        
        // 根据导出类型处理
        switch (exportType) {
            case "BOOKS":
                String filename = format.equals("CSV") ? "books.csv" : "books.xlsx";
                response.setHeader("Content-Disposition", "attachment; filename=" + filename);
                // 简化实现：先确保页面能正常工作
                response.getWriter().write("图书数据导出功能");
                break;
            case "USERS":
                filename = format.equals("CSV") ? "users.csv" : "users.xlsx";
                response.setHeader("Content-Disposition", "attachment; filename=" + filename);
                // 简化实现
                response.getWriter().write("用户数据导出功能");
                break;
            case "BORROW_RECORDS":
                // 未实现借阅记录导出
                response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED, "借阅记录导出功能未实现");
                break;
            case "ALL":
                // 未实现全部导出
                response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED, "全部数据导出功能未实现");
                break;
            default:
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "无效的导出类型");
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