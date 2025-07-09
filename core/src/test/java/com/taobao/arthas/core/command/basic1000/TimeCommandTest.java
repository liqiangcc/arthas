package com.taobao.arthas.core.command.basic1000;

import com.taobao.arthas.core.command.model.TimeModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.middleware.cli.CLI;
import com.taobao.middleware.cli.CommandLine;
import com.taobao.middleware.cli.annotations.CLIConfigurator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;

/**
 * Test for TimeCommand
 * 
 * @author example
 */
public class TimeCommandTest {
    
    private static CLI cli = null;
    
    @Before
    public void before() {
        cli = CLIConfigurator.define(TimeCommand.class);
    }
    
    @Test
    public void testBasicTimeCommand() {
        List<String> args = Arrays.asList();
        TimeCommand timeCommand = new TimeCommand();
        CommandLine commandLine = cli.parse(args, true);
        
        try {
            CLIConfigurator.inject(commandLine, timeCommand);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        
        // Mock CommandProcess
        CommandProcess process = Mockito.mock(CommandProcess.class);
        
        // Execute command
        timeCommand.process(process);
        
        // Verify that appendResult was called
        Mockito.verify(process).appendResult(Mockito.any(TimeModel.class));
        Mockito.verify(process).end();
    }
    
    @Test
    public void testTimeCommandWithFormat() {
        List<String> args = Arrays.asList("-f", "yyyy-MM-dd");
        TimeCommand timeCommand = new TimeCommand();
        CommandLine commandLine = cli.parse(args, true);
        
        try {
            CLIConfigurator.inject(commandLine, timeCommand);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        
        // Mock CommandProcess
        CommandProcess process = Mockito.mock(CommandProcess.class);
        
        // Execute command
        timeCommand.process(process);
        
        // Verify that appendResult was called
        Mockito.verify(process).appendResult(Mockito.any(TimeModel.class));
        Mockito.verify(process).end();
    }
    
    @Test
    public void testTimeCommandWithTimezone() {
        List<String> args = Arrays.asList("-z");
        TimeCommand timeCommand = new TimeCommand();
        CommandLine commandLine = cli.parse(args, true);
        
        try {
            CLIConfigurator.inject(commandLine, timeCommand);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        
        // Mock CommandProcess
        CommandProcess process = Mockito.mock(CommandProcess.class);
        
        // Execute command
        timeCommand.process(process);
        
        // Verify that appendResult was called
        Mockito.verify(process).appendResult(Mockito.any(TimeModel.class));
        Mockito.verify(process).end();
    }
    
    @Test
    public void testTimeCommandWithBothOptions() {
        List<String> args = Arrays.asList("-f", "HH:mm:ss", "-z");
        TimeCommand timeCommand = new TimeCommand();
        CommandLine commandLine = cli.parse(args, true);
        
        try {
            CLIConfigurator.inject(commandLine, timeCommand);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        
        // Mock CommandProcess
        CommandProcess process = Mockito.mock(CommandProcess.class);
        
        // Execute command
        timeCommand.process(process);
        
        // Verify that appendResult was called
        Mockito.verify(process).appendResult(Mockito.any(TimeModel.class));
        Mockito.verify(process).end();
    }
    
    @Test
    public void testTimeModelCreation() {
        String testTime = "2023-12-07 10:30:00";
        String testTimezoneId = "Asia/Shanghai";
        String testTimezoneName = "China Standard Time";
        boolean showTimezone = true;
        
        TimeModel timeModel = new TimeModel(testTime, testTimezoneId, testTimezoneName, showTimezone);
        
        Assert.assertEquals("time", timeModel.getType());
        Assert.assertEquals(testTime, timeModel.getCurrentTime());
        Assert.assertEquals(testTimezoneId, timeModel.getTimezoneId());
        Assert.assertEquals(testTimezoneName, timeModel.getTimezoneName());
        Assert.assertTrue(timeModel.isShowTimezone());
    }
}
