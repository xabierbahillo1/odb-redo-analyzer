package oracle.redo_monitor.controller;

import oracle.redo_monitor.service.SQLiteService;
import oracle.redo_monitor.utils.Constants;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/reports")
public class ReportsController {

    @Autowired
    private SQLiteService sqliteService;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(Constants.DATE_PATTERN_JAVA);

    @GetMapping({"", "/"})
    public String showReports(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            Model model) {

        LocalDate to = (toDate != null && !toDate.isEmpty()) ? LocalDate.parse(toDate, FORMATTER) : LocalDate.now();
        LocalDate from = (fromDate != null && !fromDate.isEmpty()) ? LocalDate.parse(fromDate, FORMATTER) : to.minus(30, ChronoUnit.DAYS);

        model.addAttribute("sessions", sqliteService.getDailySessionsBetweenDates(from, to));
        model.addAttribute("fromDate", from.format(FORMATTER));
        model.addAttribute("toDate", to.format(FORMATTER));
        model.addAttribute("activePage", "reports");
        return "reports";
    }

    @GetMapping("/detail/{id}")
    public String showDetail(@PathVariable int id, Model model) {
        model.addAttribute("sessionUsage", sqliteService.getSessionsByDailyId(id));
        model.addAttribute("date", sqliteService.getDateByDailyId(id));
        model.addAttribute("activePage", "reports");
        return "detail";
    }
}
