package org.word.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.word.dto.Table;
import org.word.service.WordService;

import java.util.List;
import java.util.Map;

/**
 * Created by XiuYin.Cui on 2018/1/11.
 */
@Controller
public class WordController {

    @Autowired
    private WordService tableService;

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
}
