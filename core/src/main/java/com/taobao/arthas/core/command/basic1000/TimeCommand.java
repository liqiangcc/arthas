package com.taobao.arthas.core.command.basic1000;

import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.model.TimeModel;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

/**
 * Time command - display current time and timezone information
 * 
 * @author example
 */
@Name("time")
@Summary("Display current time and timezone information")
@Description("\nExamples:\n" +
        "  time\n" +
        "  time -f\n" +
        "  time --format yyyy-MM-dd\n" +
        Constants.WIKI + Constants.WIKI_HOME + "time")
public class TimeCommand extends AnnotatedCommand {
    
    private String format;
    private boolean showTimezone = false;
    
    @Option(shortName = "f", longName = "format", argName = "format")
    @Description("Time format pattern (e.g., yyyy-MM-dd HH:mm:ss)")
    public void setFormat(String format) {
        this.format = format;
    }
    
    @Option(shortName = "z", longName = "timezone", flag = true)
    @Description("Show timezone information")
    public void setShowTimezone(boolean showTimezone) {
        this.showTimezone = showTimezone;
    }

    @Override
    public void process(CommandProcess process) {
        try {
            LocalDateTime now = LocalDateTime.now();
            String timeString;
            
            if (format != null && !format.isEmpty()) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
                timeString = now.format(formatter);
            } else {
                timeString = now.toString();
            }
            
            TimeZone timezone = TimeZone.getDefault();
            
            TimeModel timeModel = new TimeModel(timeString, timezone.getID(),
                                              timezone.getDisplayName(), showTimezone);
            
            process.appendResult(timeModel);
            process.end();
            
        } catch (Exception e) {
            process.end(-1, "Error getting time: " + e.getMessage());
        }
    }
}
