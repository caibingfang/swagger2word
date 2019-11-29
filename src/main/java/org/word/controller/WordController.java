package org.word.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.word.dto.Table;
import org.word.service.WordService;

import javax.servlet.http.HttpServletRequest;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

/**
 * Created by XiuYin.Cui on 2018/1/11.
 */
@Controller
public class WordController {

    @Autowired
    private WordService tableService;

    @Autowired
    private HttpServletRequest request;

    /**
     * @param model
     * @return
     * @see #(Model)
     */
    @Deprecated
    @RequestMapping("/toWord")
    public String getWord(Model model, @RequestParam(value = "url", required = false) String url) {
        Map<String,List<Table>> tables = tableService.tableList(url);
        model.addAttribute("maps", tables);
        return "word";
    }

    @RequestMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam(value = "url", required = true) String url) throws Exception {
        String target = request.getScheme() +"://" + request.getServerName()
                + ":" +request.getServerPort()
                + "/toWord?url="+url.trim();
        URL targetUrl = new URL(target);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.add("Content-Disposition", "attachment; filename=swagger.doc;filename*=utf-8''"+ URLEncoder.encode("swagger.doc","UTF-8"));
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(FileCopyUtils.copyToByteArray(targetUrl.openStream()));


    }



}
