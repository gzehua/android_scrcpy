package com.genymobile.scrcpy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class Server {

    private static String ip = null;
    public static final String SERVER_PATH;

    static {
        String[] classPaths = System.getProperty("java.class.path").split(File.pathSeparator);
        // By convention, scrcpy is always executed with the absolute path of scrcpy-server.jar as the first item in the classpath
        SERVER_PATH = classPaths[0];
    }

    private static class Completion {
        private int running;
        private boolean fatalError;

        Completion(int running) {
            this.running = running;
        }

        synchronized void addCompleted(boolean fatalError) {
            --running;
            if (fatalError) {
                this.fatalError = true;
            }
            if (running == 0 || this.fatalError) {
                notify();
            }
        }

        synchronized void await() {
            try {
                while (running > 0 && !fatalError) {
                    wait();
                }
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }


    private Server() {
        // not instantiable
    }

    private static void scrcpy(Options options) throws IOException, ConfigurationException {
        CleanUp cleanUp = null;
        Thread initThread = null;

//        if (options.getCleanup()) {
//            cleanUp = CleanUp.configure(options.getDisplayId());
//            initThread = startInitThread(options, cleanUp);
//        }
        boolean control = options.getControl();
        boolean video = options.getVideo();
        final Device device = new Device(options);
        List<AsyncProcessor> asyncProcessors = new ArrayList<>();
        try (DesktopConnection connection = DesktopConnection.open(ip)) {

            if (video){
                SurfaceEncoder screenEncoder = new SurfaceEncoder(options.getVideoBitRate(),device,connection.getVideoOutput());
                asyncProcessors.add(screenEncoder);
            }

            if (control) {
                ControlChannel controlChannel = connection.getControlChannel();
                Controller controller = new Controller(device, controlChannel, cleanUp, options.getClipboardAutosync(), options.getPowerOn());
                device.setClipboardListener(text -> {
//                    DeviceMessage msg = DeviceMessage.createClipboard(text);
//                    controller.getSender().send(msg);
                });
                asyncProcessors.add(controller);
            }

            Completion completion = new Completion(asyncProcessors.size());
            for (AsyncProcessor asyncProcessor : asyncProcessors) {
                asyncProcessor.start((fatalError) -> {
                    completion.addCompleted(fatalError);
                });
            }

            completion.await();
        } finally {
            if (initThread != null) {
                initThread.interrupt();
            }
            for (AsyncProcessor asyncProcessor : asyncProcessors) {
                asyncProcessor.stop();
            }

            try {
                if (initThread != null) {
                    initThread.join();
                }
                for (AsyncProcessor asyncProcessor : asyncProcessors) {
                    asyncProcessor.join();
                }
            } catch (InterruptedException e) {
                // ignore
            }
        }

    }


    public static void main(String... args) throws Exception {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                Ln.e("Exception on thread " + t, e);
            }
        });

        try {
            Process cmd = Runtime.getRuntime().exec("rm /data/local/tmp/scrcpy-server.jar");
            cmd.waitFor();
        } catch (IOException e1) {
            e1.printStackTrace();
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }

        Options options = Options.parse(args);
        scrcpy(options);
    }
}

