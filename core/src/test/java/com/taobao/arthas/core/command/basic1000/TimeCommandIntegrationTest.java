package com.taobao.arthas.core.command.basic1000;

import com.taobao.arthas.core.command.model.TimeModel;
import com.taobao.arthas.core.shell.command.Command;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.middleware.cli.CLI;
import com.taobao.middleware.cli.CommandLine;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Integration test for TimeCommand
 * Tests the complete command execution flow
 * 
 * @author example
 */
public class TimeCommandIntegrationTest {
    
    @Test
    public void testTimeCommandExecution() throws InterruptedException {
        // Create command instance
        Command timeCommand = Command.create(TimeCommand.class);
        Assert.assertEquals("time", timeCommand.name());
        
        // Mock session and process
        Session session = Mockito.mock(Session.class);
        CommandProcess process = Mockito.mock(CommandProcess.class);
        
        // Mock command line with no arguments
        CLI cli = timeCommand.cli();
        CommandLine commandLine = cli.parse(Arrays.asList(new String[]{}));
        
        Mockito.when(process.session()).thenReturn(session);
        Mockito.when(process.commandLine()).thenReturn(commandLine);
        Mockito.when(process.args()).thenReturn(Arrays.asList());
        
        // Use CountDownLatch to wait for async execution
        CountDownLatch latch = new CountDownLatch(1);
        Mockito.doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(process).end();
        
        // Execute command
        timeCommand.processHandler().handle(process);
        
        // Wait for completion
        Assert.assertTrue("Command should complete within 5 seconds", 
                         latch.await(5, TimeUnit.SECONDS));
        
        // Verify that appendResult was called with TimeModel
        ArgumentCaptor<TimeModel> captor = ArgumentCaptor.forClass(TimeModel.class);
        Mockito.verify(process).appendResult(captor.capture());
        
        TimeModel result = captor.getValue();
        Assert.assertNotNull("TimeModel should not be null", result);
        Assert.assertEquals("time", result.getType());
        Assert.assertNotNull("Current time should not be null", result.getCurrentTime());
        Assert.assertFalse("Should not show timezone by default", result.isShowTimezone());
        
        Mockito.verify(process).end();
    }
    
    @Test
    public void testTimeCommandWithTimezoneOption() throws InterruptedException {
        // Create command instance
        Command timeCommand = Command.create(TimeCommand.class);
        
        // Mock session and process
        Session session = Mockito.mock(Session.class);
        CommandProcess process = Mockito.mock(CommandProcess.class);
        
        // Mock command line with timezone option
        CLI cli = timeCommand.cli();
        CommandLine commandLine = cli.parse(Arrays.asList("-z"));
        
        Mockito.when(process.session()).thenReturn(session);
        Mockito.when(process.commandLine()).thenReturn(commandLine);
        Mockito.when(process.args()).thenReturn(Arrays.asList("-z"));
        
        // Use CountDownLatch to wait for async execution
        CountDownLatch latch = new CountDownLatch(1);
        Mockito.doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(process).end();
        
        // Execute command
        timeCommand.processHandler().handle(process);
        
        // Wait for completion
        Assert.assertTrue("Command should complete within 5 seconds", 
                         latch.await(5, TimeUnit.SECONDS));
        
        // Verify that appendResult was called with TimeModel
        ArgumentCaptor<TimeModel> captor = ArgumentCaptor.forClass(TimeModel.class);
        Mockito.verify(process).appendResult(captor.capture());
        
        TimeModel result = captor.getValue();
        Assert.assertNotNull("TimeModel should not be null", result);
        Assert.assertEquals("time", result.getType());
        Assert.assertNotNull("Current time should not be null", result.getCurrentTime());
        Assert.assertTrue("Should show timezone when -z option is used", result.isShowTimezone());
        Assert.assertNotNull("Timezone ID should not be null", result.getTimezoneId());
        Assert.assertNotNull("Timezone name should not be null", result.getTimezoneName());
        
        Mockito.verify(process).end();
    }
    
    @Test
    public void testTimeCommandWithFormatOption() throws InterruptedException {
        // Create command instance
        Command timeCommand = Command.create(TimeCommand.class);
        
        // Mock session and process
        Session session = Mockito.mock(Session.class);
        CommandProcess process = Mockito.mock(CommandProcess.class);
        
        // Mock command line with format option
        CLI cli = timeCommand.cli();
        CommandLine commandLine = cli.parse(Arrays.asList("-f", "yyyy-MM-dd"));
        
        Mockito.when(process.session()).thenReturn(session);
        Mockito.when(process.commandLine()).thenReturn(commandLine);
        Mockito.when(process.args()).thenReturn(Arrays.asList("-f", "yyyy-MM-dd"));
        
        // Use CountDownLatch to wait for async execution
        CountDownLatch latch = new CountDownLatch(1);
        Mockito.doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(process).end();
        
        // Execute command
        timeCommand.processHandler().handle(process);
        
        // Wait for completion
        Assert.assertTrue("Command should complete within 5 seconds", 
                         latch.await(5, TimeUnit.SECONDS));
        
        // Verify that appendResult was called with TimeModel
        ArgumentCaptor<TimeModel> captor = ArgumentCaptor.forClass(TimeModel.class);
        Mockito.verify(process).appendResult(captor.capture());
        
        TimeModel result = captor.getValue();
        Assert.assertNotNull("TimeModel should not be null", result);
        Assert.assertEquals("time", result.getType());
        Assert.assertNotNull("Current time should not be null", result.getCurrentTime());
        
        // Verify the format is applied (should match yyyy-MM-dd pattern)
        String timeString = result.getCurrentTime();
        Assert.assertTrue("Time should match yyyy-MM-dd format", 
                         timeString.matches("\\d{4}-\\d{2}-\\d{2}"));
        
        Mockito.verify(process).end();
    }
}
