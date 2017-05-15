package org.cytoscape.tmm;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.cytoscape.application.CyApplicationConfiguration;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.command.CommandExecutorTaskFactory;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.*;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.session.CySessionManager;
import org.cytoscape.tmm.Enums.ETMMProps;
import org.cytoscape.tmm.gui.TMMPanel;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.swing.DialogTaskManager;
import org.cytoscape.task.write.ExportNetworkViewTaskFactory;
import org.osgi.framework.BundleContext;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Scanner;

public class TMMActivator extends AbstractCyActivator {
    public static CySwingApplication cytoscapeDesktopService;
    public static DialogTaskManager taskManager;
    public static TaskManager justTaskManager;
    public static SynchronousTaskManager synchTaskManager;
    public static CySessionManager cySessionManager;
    public static CyNetworkFactory networkFactory;
    public static CyNetworkViewFactory networkViewFactory;
    public static CyNetworkManager networkManager;
    public static CyNetworkViewManager networkViewManager;
    public static VisualMappingManager visualMappingManager;
    public static VisualMappingFunctionFactory vmfFactoryC;
    public static VisualMappingFunctionFactory vmfFactoryD;
    public static VisualMappingFunctionFactory vmfFactoryP;
    public static VisualStyleFactory visualStyleFactory;
    public static CyTableFactory tableFactory;
    public static CyApplicationConfiguration cyAppConfig;
    public static CyEventHelper cyEventHelper;
    public static CyApplicationManager cyApplicationManager;
    public static CyTableManager cyTableManager;
    public static CyNetworkTableManager cyNetworkTableManager;
    public static CommandExecutorTaskFactory commandExecutor;
    public static ExportNetworkViewTaskFactory exportNetworkViewTaskFactory;

    public static TMMPanel tmmPanel;
    private static File TMMDir;
    private static String webPageUrl = "http://big.sci.am/software/tmm/";


    private static Logger TMMLogger;
    private static File logFile;
    private static String logName = "TMM.log";

    private static String tmmPropsFileName = "tmm.props";
    private static File tmmPropsFile = null;
    private static String aboutText;
    private static String aboutFileName = "about.txt";
    private static String userManualFileName = "TMM_User_Guide.pdf";
    private static String userManualURL = "http://big.sci.am/apps/TMM/TMM_User_Guide_0.1.pdf";
    private static Properties tmmProps;




    @Override
    public void start(BundleContext bc) throws Exception {
        cytoscapeDesktopService = getService(bc, CySwingApplication.class);
        taskManager = getService(bc, DialogTaskManager.class);
        justTaskManager = getService(bc, TaskManager.class);
        synchTaskManager = getService(bc, SynchronousTaskManager.class);
        cySessionManager = getService(bc, CySessionManager.class);
        networkFactory = getService(bc, CyNetworkFactory.class);
        networkViewFactory = getService(bc, CyNetworkViewFactory.class);
        networkManager = getService(bc, CyNetworkManager.class);
        networkViewManager = getService(bc, CyNetworkViewManager.class);
        visualMappingManager = getService(bc, VisualMappingManager.class);
        vmfFactoryC = getService(bc, VisualMappingFunctionFactory.class, "(mapping.type=continuous)");
        vmfFactoryD = getService(bc, VisualMappingFunctionFactory.class, "(mapping.type=discrete)");
        vmfFactoryP = getService(bc, VisualMappingFunctionFactory.class, "(mapping.type=passthrough)");
        visualStyleFactory = getService(bc, VisualStyleFactory.class);
        tableFactory = getService(bc, CyTableFactory.class);
        cyAppConfig = getService(bc, CyApplicationConfiguration.class);
        cyEventHelper = getService(bc, CyEventHelper.class);
        cyApplicationManager = getService(bc, CyApplicationManager.class);
        cyTableManager = getService(bc, CyTableManager.class);
        cyNetworkTableManager = getService(bc, CyNetworkTableManager.class);
        cySessionManager = getService(bc, CySessionManager.class);
        tmmPanel = new TMMPanel();
        commandExecutor = getService(bc, CommandExecutorTaskFactory.class);
        exportNetworkViewTaskFactory = getService(bc, ExportNetworkViewTaskFactory.class);

        registerService(bc, cytoscapeDesktopService, CySwingApplication.class, new Properties());
        registerService(bc, taskManager, DialogTaskManager.class, new Properties());
        registerService(bc, justTaskManager, TaskManager.class, new Properties());
        registerService(bc, synchTaskManager, SynchronousTaskManager.class, new Properties());
        registerService(bc, cySessionManager, CySessionManager.class, new Properties());
        registerService(bc, networkFactory, CyNetworkFactory.class, new Properties());
        registerService(bc, networkViewFactory, CyNetworkViewFactory.class, new Properties());
        registerService(bc, networkViewManager, CyNetworkViewManager.class, new Properties());
        registerService(bc, networkManager, CyNetworkManager.class, new Properties());
        registerService(bc, visualMappingManager, VisualMappingManager.class, new Properties());
        registerService(bc, vmfFactoryC, VisualMappingFunctionFactory.class, new Properties());
        registerService(bc, vmfFactoryD, VisualMappingFunctionFactory.class, new Properties());
        registerService(bc, vmfFactoryP, VisualMappingFunctionFactory.class, new Properties());
        registerService(bc, visualStyleFactory, VisualStyleFactory.class, new Properties());
        registerService(bc, tableFactory, CyTableFactory.class, new Properties());
        registerService(bc, cyAppConfig, CyApplicationConfiguration.class, new Properties());
        registerService(bc, cyEventHelper, CyEventHelper.class, new Properties());
        registerService(bc, cyApplicationManager, CyApplicationManager.class, new Properties());
        registerService(bc, cyTableManager, CyTableManager.class, new Properties());
        registerService(bc, cyNetworkTableManager, CyNetworkTableManager.class, new Properties());
        registerService(bc, exportNetworkViewTaskFactory, ExportNetworkViewTaskFactory.class, new Properties());
        registerService(bc, tmmPanel, CytoPanelComponent.class, new Properties());
    }

    public static Properties getTMMProps() {
        if (tmmProps == null)
            initProperties();
        return tmmProps;
    }
    private static void initProperties() {
        tmmPropsFile = new File(getTMMDir(), tmmPropsFileName);
        FileInputStream stream = null;
        if (tmmPropsFile.exists())
            try {
                stream = new FileInputStream(getTMMDir().getAbsolutePath() + "/" + tmmPropsFileName);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        boolean isPropsFileValid = true;

        if (stream != null) {
            if (tmmProps == null) {
                tmmProps = new Properties();
                try {
                    tmmProps.load(stream);
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    TMMLogger.error(e.getMessage());
                }
            }

            for (ETMMProps etmmProps : ETMMProps.values()) {
                if (!tmmProps.containsKey(etmmProps.getName())) {
                    isPropsFileValid = false;
                    break;
                }
            }
            if (isPropsFileValid)
                for (ETMMProps props : ETMMProps.values()) {
                    if (tmmProps.getProperty(props.getName()) == null) {
                        isPropsFileValid = false;
                        break;
                    }
                }
        } else
            isPropsFileValid = false;

        if (!isPropsFileValid) {
            try {
                if (tmmPropsFile.exists())
                    tmmPropsFile.delete();
                tmmPropsFile.createNewFile();
                ClassLoader cl = TMMActivator.class.getClassLoader();
                InputStream in = cl.getResourceAsStream(tmmPropsFileName);
                tmmProps = new Properties();
                tmmProps.load(in);
                tmmProps.store(new PrintWriter(getTMMPropsFile()), "");
            } catch (IOException e) {
                TMMLogger.error(e.getMessage());
                e.printStackTrace();
            }
        }

        for (ETMMProps property : ETMMProps.values()) {
            property.setOldValue(Boolean.parseBoolean((String) getTMMProps().get(property.getName())));
            property.setNewValue(Boolean.parseBoolean((String) getTMMProps().get(property.getName())));
        }

    }

    public static File getTMMPropsFile() {
        if (tmmPropsFile == null)
            initProperties();
        return tmmPropsFile;
    }

    public static File getTMMDir() {
        if (TMMDir == null) {
            createPluginDirectory();
        }
        return TMMDir;
    }

    private static void createPluginDirectory() {
        File appConfigDir = cyAppConfig.getConfigurationDirectoryLocation();
        File appData = new File(appConfigDir, "app-data");
        if (!appData.exists())
            appData.mkdir();

        TMMDir = new File(appData, "TMM");
        if (!TMMDir.exists())
            if (!TMMDir.mkdir())
                LoggerFactory.getLogger(TMMActivator.class).
                        error("Failed to create directory " + TMMDir.getAbsolutePath());

    }
    public static File getRecentDirectory() {
        File recentDirFile = new File(TMMActivator.getTMMDir(), "recentDir.txt");
        File recentDirectory = getRecentDirectoryFile();
        try {
            Scanner scanner = new Scanner(recentDirFile);
            if (scanner.hasNextLine())
                recentDirectory = new File(scanner.nextLine());
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }
        if (recentDirectory == null)
            recentDirectory = TMMActivator.getTMMDir();
        return recentDirectory;
    }

    private static File getRecentDirectoryFile() {
        File recentDirFile = new File(TMMActivator.getTMMDir(), "recentDir.txt");
        if (!recentDirFile.exists())
            try {
                recentDirFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

        return recentDirFile;
    }

    public static void writeRecentDirectory(String selectedFilePath) {
        try {
            PrintWriter recentDirWriter = new PrintWriter(getRecentDirectoryFile());
            recentDirWriter.write(selectedFilePath);
            recentDirWriter.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static Logger getLogger() {
        if (TMMLogger != null)
            return TMMLogger;
        File loggingDir = null;

        if (logFile == null)
            loggingDir = setLoggingDirectory();
        if (loggingDir != null && loggingDir.exists()) {
            logFile = new File(loggingDir, logName);
            if (!logFile.exists())
                try {
                    logFile.createNewFile();
                } catch (IOException e) {
                    LoggerFactory.getLogger(TMMActivator.class).error(e.getMessage());
                }
            else {
                if (logFile.length() > (1024 * 1024)) {
                    try {
                        DateFormat dateFormat = new SimpleDateFormat("HH_mm_dd_MM_yy");
                        boolean success = logFile.renameTo(new File(loggingDir, logFile.getName() + dateFormat.format(new Date())));

                        if (success) {
                            if (!logFile.createNewFile())
                                LoggerFactory.getLogger(TMMActivator.class).error("Could not create new TMM log file");
                        } else
                            LoggerFactory.getLogger(TMMActivator.class).error("Could not rename log file");
                    } catch (IOException e) {
                        LoggerFactory.getLogger(TMMActivator.class).error(e.getMessage());
                    }
                }
            }
        }
        TMMLogger = Logger.getLogger(TMMActivator.class);

        try {
            TMMLogger.addAppender(new FileAppender(new PatternLayout(), logFile.getAbsolutePath(), true));
        } catch (IOException e) {
            LoggerFactory.getLogger(TMMActivator.class).error(e.getMessage());
        }
        return TMMLogger;
    }

    private static File setLoggingDirectory() {
        File loggingDir = new File(getTMMDir(), "logs");
        boolean dirValid = true;
        if (!loggingDir.exists())
            dirValid = loggingDir.mkdir();
        if (dirValid)
            return loggingDir;
        return null;
    }

    public static String getAboutText() {
        if (aboutText == null) {
            aboutText = "";
            ClassLoader classLoader = TMMActivator.class.getClassLoader();
            InputStream inputStream = classLoader.getResourceAsStream(aboutFileName);
            Reader reader = new InputStreamReader(inputStream);
            StringBuilder stringBuilder = new StringBuilder();
            char[] chars = new char[1024];
            try {
                while ((reader.read(chars) > 0)) {
                    stringBuilder.append(chars);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            aboutText = stringBuilder.toString();
        }
        return aboutText;
    }

    public static String getUserManualFileName() {
        return userManualFileName;
    }

    public static String getUserManualURL() {
        return userManualURL;
    }

    public static String getWebPageUrl() {
        return webPageUrl;
    }
}
