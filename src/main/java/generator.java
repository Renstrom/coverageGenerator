import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;


import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class generator {


    public static ScheduledExecutorService minuteTimer() {
        Runnable timer = new Runnable() {
            int minutes = 0;
            int seconds = 0;

            @Override
            public void run() {
                seconds++;
                if (seconds % 60 == 0) {
                    minutes++;
                }
                int second = seconds % 60;

                System.out.print("Time Elapsed " + minutes + ":" + second + "\r");
            }
        };
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(timer, 1, 1, TimeUnit.SECONDS);
        return executor;
    }



    /**
     * ,git -C ~/>REPONAME< stash, git -C />REPONAME checkout commit id,
     */
    public static void runCMDCommands(String repoPath, String[] commands, String gitID) {
        String zeroCommand = "git -C " + repoPath + " clean -fd";
        String firstCommand = "git -C " + repoPath + " restore *";
        String secondCommand = "git -C " + repoPath + " checkout" + gitID;

        System.out.println(repoPath);
        try {
            System.out.println("Cleaning");
            Process process = null;
            try {
                process = Runtime.getRuntime().exec(zeroCommand);
            } catch (IOException e) {
                e.printStackTrace();
            }
            assert process != null;
            process.waitFor();
            System.out.println("Stashing");
            try {
                process = Runtime.getRuntime().exec(firstCommand);
            } catch (IOException e) {
                e.printStackTrace();
            }
            process.waitFor();
            System.out.println("Checkout");
            try {
                process = Runtime.getRuntime().exec(secondCommand);
            } catch (IOException e) {
                e.printStackTrace();
            }
            process.waitFor();

            File newFile = new File(repoPath+"/pom.xml");
            BufferedReader r = new BufferedReader(new FileReader(newFile));
            String pomContent = r.lines().collect(Collectors.joining(System.lineSeparator()));
            if(!pomContent.contains("jacoco")) return;
            pomContent = pomContent.replaceAll("<jacoco.skip>true</jacoco.skip>","<jacoco.skip>false</jacoco.skip>");
            r.close();
            FileOutputStream w = new FileOutputStream(repoPath+"/pom.xml");
            w.write(pomContent.getBytes(StandardCharsets.UTF_8));
            w.close();
            for (String command : commands) {
                System.out.println("Running command: " + command);
                String[] cmd = {"/bin/sh", "-c", "(cd " + repoPath + " ; git status; " + command + ")"};
                try {
                    ProcessBuilder builder = new ProcessBuilder(cmd);
                    Process loopProcess = builder.start();
                    ScheduledExecutorService executor = minuteTimer();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(loopProcess.getInputStream()));
                    while (reader.readLine() != null) {}
                    loopProcess.waitFor();

                    executor.shutdown();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<String> getDetailedInformation(String jgitMinerPath, String projectName) {
        ArrayList<String> output = new ArrayList<>();
        String completePath = jgitMinerPath + "/result/detailedInformation_" + projectName + ".txt";
        try {
            BufferedReader reader =
                    new BufferedReader(new FileReader(completePath));
            String line;

            while ((line = reader.readLine()) != null) {
                output.add(line);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return output;
    }

    public static ArrayList<String> getInformation() {
        ArrayList<String> output = new ArrayList<>();
        try {
            BufferedReader reader =
                    new BufferedReader(new FileReader("input.txt"));
            String line;

            while ((line = reader.readLine()) != null) {
                output.add(line);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return output;
    }

    public static String getCoverage(String repoPath, String directoryPath) {
        return parser.getOverallResult(repoPath + "/" + directoryPath);
    }



    public static void addCoverage(ArrayList<String> detailedInformation, String repoPath, String[] commands) {
        String gitID = "";
        String gitID1 = "";
        String directoryPath;
        int size = detailedInformation.size() * 2;
        int current = 0;

        for (int i = 0; i < detailedInformation.size(); i++) {
            current++;
            System.out.println("Running  " + current + " of " + size);
            String[] line = detailedInformation.get(i).split(" ");
            if (!line[0].equals(gitID)) {
                gitID = line[0];
                runCMDCommands(repoPath, commands, gitID);
            }
            directoryPath = line[2];
            String coverage = getCoverage(repoPath, directoryPath);

            detailedInformation.set(i, detailedInformation.get(i) + " " + coverage);
        }
        for (int i = 0; i < detailedInformation.size(); i++) {
            current++;
            System.out.println("Running  " + current + " of " + size);
            String[] line = detailedInformation.get(i).split(" ");
            if (!line[1].equals(gitID)) {
                gitID = line[1];
                runCMDCommands(repoPath, commands, gitID);
            }
            directoryPath = line[2];
            String coverage = getCoverage(repoPath, directoryPath);

            detailedInformation.set(i, detailedInformation.get(i) + " " + coverage);
        }
    }

    public static void printResult(ArrayList<String> output) {
        for (String line : output) {
            System.out.println(line);
        }
    }

    /**
     * index -1 = branch coverage of output
     * index -2 = code coverage of output
     * @param output
     */
    public static void saveResults(ArrayList<String> output,String jgitMinerPath, String projectName){
        String completePath = jgitMinerPath + "/result/detailedInformation_" + projectName + ".txt";

        try{
            BufferedWriter w = new BufferedWriter(new FileWriter(completePath,false));
            for( String o: output){
                String[] result = o.split(" ");

                boolean branchCoverageDecrease = Double.parseDouble(result[result.length-1])> Double.parseDouble(result[result.length-3]);
                boolean codeCoverageDecrease = Double.parseDouble(result[result.length-2])> Double.parseDouble(result[result.length-4]);
                for (int i = 0; i < result.length; i++) {
                    if(i==result.length-3){
                        if(branchCoverageDecrease){
                            w.write("("+result[i]+ ") ");
                        } else {
                            w.write(""+result[i]+ " ");
                        }
                    } else if(i==result.length-4){
                        if(codeCoverageDecrease){
                            w.write("("+result[i]+ ") ");
                        } else {
                            w.write(result[i]+ " ");
                        }
                    } else {
                        w.write(result[i]+ " ");
                    }
                }
                w.write("\n");
            }
            w.close();
        } catch (IOException e){
            e.printStackTrace();
        }

    }


    public static void main(String[] args) {
        ArrayList<String> information = getInformation();
        for (String info : information) {
            String[] input = info.split(",");
            if (input.length < 3) {
                continue;
            }

            System.out.println("Generating test coverage for " + input[0]);
            String[] commands = new String[input.length - 3];
            String name = input[0];
            String repoPath = input[1];
            String jgitMinerPath = input[2];
            System.arraycopy(input, 3, commands, 0, input.length - 3);
            ArrayList<String> detailedInformation = getDetailedInformation(jgitMinerPath, name);
            addCoverage(detailedInformation, repoPath, commands);
            saveResults(detailedInformation,jgitMinerPath,name);

            printResult(detailedInformation);


        }
    }
}
