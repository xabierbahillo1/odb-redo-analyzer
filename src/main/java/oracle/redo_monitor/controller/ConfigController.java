package oracle.redo_monitor.controller;

import oracle.redo_monitor.model.OracleCredentials;
import oracle.redo_monitor.service.OracleCredentialsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/config")
public class ConfigController {

    @Autowired
    private OracleCredentialsService credentialsService;

    @GetMapping({"", "/"})
    public String showConfig(Model model) throws Exception {
        OracleCredentials creds = credentialsService.loadCredentialsEncrypted();
        model.addAttribute("creds", creds);
        model.addAttribute("activePage", "config");
        return "config";
    }

    @PostMapping({"", "/"})
    public String saveConfig(@RequestParam String url,
                             @RequestParam String username,
                             @RequestParam String password,
                             @RequestParam String role,
                             Model model) {
        try {
            credentialsService.saveCredentials(url, username, password, role);
            model.addAttribute("message", "Configuration saved successfully.");
        } catch (Exception e) {
            model.addAttribute("error", "Error saving configuration: " + e.getMessage());
        }
        try {
            model.addAttribute("creds", credentialsService.loadCredentialsEncrypted());
        } catch (Exception ignored) {}
        return "config";
    }
}
