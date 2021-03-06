/*
 * Copyright 2013 Centro de Investigación en Tecnoloxías da Información (CITIUS),
 * University of Santiago de Compostela (USC) http://citius.usc.es.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package es.usc.citius.composit.cli;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.base.Strings;
import com.google.common.io.CharStreams;
import es.usc.citius.composit.cli.command.CliCommand;
import org.fusesource.jansi.AnsiConsole;
import org.reflections.Reflections;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.fusesource.jansi.Ansi.ansi;

/**
 * @author Pablo Rodríguez Mier <<a href="mailto:pablo.rodriguez.mier@usc.es">pablo.rodriguez.mier@usc.es</a>>
 */
public class CompositCli {

    private static final String nl = System.getProperty("line.separator");
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(CompositCli.class);


    // Command binding
    private Map<String, CliCommand> bindings = new HashMap<String, CliCommand>();

    @Parameter(names = "--debug", description = "Change log level to Debug mode")
    private boolean debug = false;

    @Parameter(names = "--help", help = true, description = "Print general command usage options")
    private boolean showHelp = false;

    @Parameter(names = {"-v", "--version"}, description = "Print ComposIT version")
    private boolean version = false;

    @Parameter(names = {"-c", "--countdown"}, description = "Start a countdown before invoking the command")
    private Integer countdown = 0;

    @Parameter(names = {"-m", "--metrics"}, description = "Record advanced metrics")
    private boolean metrics = false;

    private JCommander cli;

    public static void main(String[] args) throws Exception {
        new CompositCli().run(args);
    }

    public CompositCli() {
        AnsiConsole.systemInstall();
        // Configure cli with the available commands
        this.cli = new JCommander(this);
        cli.setProgramName("Composit");
        // Automatic discover cli commands
        discoverAndBindCommands();
    }

    private void discoverAndBindCommands(){
        Reflections reflections = new Reflections("es.usc.citius.composit.cli.command");
        Set<Class<? extends CliCommand>> classes = reflections.getSubTypesOf(CliCommand.class);

        for(Class<? extends CliCommand> commandClass : classes){
            // Create a new instance
            try {
                // Bind command impl
                CliCommand instance = commandClass.newInstance();
                bindings.put(instance.getCommandName(), instance);
                log.debug("Command {} installed.", instance.getCommandName());
            } catch (InstantiationException e) {
                log.warn("Exception during automatic command instantiation", e);
            } catch (IllegalAccessException e) {
                log.warn("Illegal access to command constructor", e);
            }
        }
    }

    private void setLogbackLevel(Level level){
        ((Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME)).setLevel(level);
    }

    private void run(String[] args) {
        header(args);

        // Parse options
        try {
            if (args.length == 0) throw new Exception("No arguments specified");
            cli.parse(args);
        }catch(Exception e){
            errorln(e.getMessage());
            errorln("To see the commands and options available, use --help");
            System.exit(-1);
        }

        handleGlobalParameters();

        countdown(countdown);
        // Process command
        String command = "";
        try {
            command = cli.getParsedCommand();
            if (command != null && !command.isEmpty()){
                println("Invoking " + command + " command...");
                bindings.get(cli.getParsedCommand()).invoke(this);
            }
        } catch (Exception e) {
            errorln("Command " + command + " error: " + e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private void handleGlobalParameters() {
        // Configure log level
        setLogbackLevel((debug)? Level.DEBUG : Level.INFO);

        // Print help?
        if (showHelp){
            cli.usage();
            System.exit(0);
        }

        // Record metrics?

    }

    public void countdown(int seconds){
        while(seconds > 0){
            println("Starting in " + seconds + " seconds...");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) { }
            seconds--;
        }
    }

    public void println(Object obj){
        AnsiConsole.out().println(obj.toString());
    }

    public void println(String msg){;
        AnsiConsole.out().println(msg);
    }

    public void errorln(String msg){
        AnsiConsole.err().println(ansi().render("@|red " + msg + "|@"));
    }

    private void newline(){
        AnsiConsole.out().println();
    }

    public void separator(int size){
        println(Strings.repeat("=", size));
    }

    public void separator(){
        separator(80);
    }

    public void header(String[] args) {

        println(ansi().render("@|yellow " + getLogo() + "|@"));
        newline();
        println(ansi().render("\t   @|yellow ComposIT|@ " + ":: Automatic Service Composition API"));
        println("\t   Copyright(c) 2013 CITIUS http://citius.usc.es");
        newline();
        newline();
        println(ansi().render("This software is licensed under @|green Apache 2.0|@ license:"));
        newline();
        println(getLicense());
        newline();
        println("Command-line argument: " + Arrays.toString(args));
        pause(500);
    }

    public void pause(int ms){
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {}
    }

    public void systemInfo(){
        println(getSystemInfo());
        newline();
    }

    private String getSystemInfo(){
        String info =
                "Java: " + System.getProperty("java.version") + nl +
                "OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version") + " " + System.getProperty("os.arch") + nl +
                "Free/Total/Max memory: " + byteCount(Runtime.getRuntime().freeMemory(), true) + "/" + byteCount(Runtime.getRuntime().totalMemory(), true) + "/" + byteCount(Runtime.getRuntime().maxMemory(), true);
        return info;
    }

    /**
     * Util method to print formatted byte size converted.
     * Code from http://stackoverflow.com/a/3758880
     */
    public static String byteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    public String getLogo(){
        try {
            return CharStreams.toString(new InputStreamReader(
                    this.getClass().getClassLoader().getResourceAsStream("logo.txt")));
        } catch (IOException e) {
            return "";
        }
    }

    public String getLicense(){
        try {
            String mark = "\t~ ";
            String license = CharStreams.toString(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream("LICENSE.txt")));
            // Format license
            return mark + license.replaceAll("\\r?\\n", nl + mark);
        } catch (IOException e) {
            return "";
        }
    }

    public boolean isShowHelp() {
        return showHelp;
    }

    public boolean isVersion() {
        return version;
    }

    public boolean isMetrics() {
        return metrics;
    }

    public boolean isDebug() {
        return debug;
    }

    public JCommander getCli() {
        return cli;
    }
}
