
/*
 * Copyright 2016 Witold Budzynski | http://nt4.pl
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pl.nt4.linuxtools.installed.packages;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 *
 * @author witold.budzynski
 */
public class InstalledPackages {

    private final String[] args;

    private String installedByUser;
    private String installedByDate;
    private String installedByCommand;
    private String operation;
    private boolean verbose;

    /**
     * Getting command line argument by name
     * @param name 
     * @return argument value or null if not exists
     */
    public String getParam(String name) {
        for (String arg : args) {
            String[] argVal = arg.split("=");
            if (argVal.length == 2) {
                if (argVal[0].equals(name)) {
                    return argVal[1];
                }
            }
        }
        return null;
    }

    /**
     * Constructor
     * @param args command line parameters  
     */
    public InstalledPackages(String[] args) {
        this.args = args;
    }

    /**
     * Working procedure
     * @throws FileNotFoundException
     * @throws IOException 
     */
    public void run() throws FileNotFoundException, IOException {
        installedByUser = getParam("--user");
        installedByDate = getParam("--date");
        installedByCommand = getParam("--command");
        operation = getParam("--operation");
        verbose = getParam("--verbose").equals("true");

        if (installedByUser == null && installedByDate == null && installedByCommand == null) {
            System.out.println("installed packages - list easy filtered installed debian packages readed from /var/log/apt/history.log");            
            System.out.println("arguments: --user=<user> --date=<yyyy-mm-dd> --command=<command> --verbose=<true/false> --operation=<Install/Remove/Update>");
            System.out.println("No arguments\n");

            return;
        }

        if (verbose) {
            System.out.println("Arguments: ");
            if (installedByUser != null) {
                System.out.println("--user=" + installedByUser);
            }
            if (installedByDate != null) {
                System.out.println("--date=" + installedByDate);
            }
            if (installedByCommand != null) {
                System.out.println("--command" + installedByCommand);
            }
            if (operation != null) {
                System.out.println("--operation=" + operation);
            }
        }

        BufferedReader rd = new BufferedReader(
                new InputStreamReader(new FileInputStream(new File("/var/log/apt/history.log"))));
        String line;

        // some filters
        boolean installedByDateFilter = false;
        boolean installedByCommandFilter = false;
        boolean installedByUserFilter = false;
        
        while ((line = rd.readLine()) != null) {

            if (line.startsWith("Start-Date:")) {
                if (installedByDate != null) {
                    int subIdx = "Start-Date:".length();
                    String dateStr = line.substring(subIdx, subIdx + 11).trim();
                    installedByDateFilter = dateStr.equals(installedByDate);
                } else {
                    installedByDateFilter = true;
                }
                continue;
            }

            if (line.startsWith("Commandline:")) {                
                if (installedByCommand != null) {                    
                    String commandline = line.substring("Commandline: ".length());                
                    installedByCommandFilter = (commandline.equals(installedByCommand));
                } else {
                    installedByCommandFilter = true;
                }
                continue;
            }

            if (line.startsWith("Requested-By:")) {
                if (installedByUser != null) {

                    if (line.startsWith("Requested-By: ")) {
                        String userFull = line.substring("Requested-By: ".length()).trim();
                        int uidIdx = userFull.indexOf("(");
                        String user;
                        if (uidIdx > -1) {
                            user = userFull.substring(uidIdx - 1);
                        } else {
                            user = userFull;
                        }

                        installedByUserFilter = user.equals(installedByUser);
                    }

                } else {
                    installedByUserFilter = true;
                }
                continue;
            }
       
            // print filtered values
            if (line.startsWith(operation + ":")) {
                if (installedByDateFilter && installedByCommandFilter && installedByUserFilter) {
                    String packages = line.substring((operation + ": ").length());

                    if (verbose) {
                        System.out.println(line);
                    }
                    int startIdx = 0;
                    
                    while (true) {
                        int bracketIdx = packages.indexOf('(', startIdx);
                        if (bracketIdx == -1) {
                            break;
                        }
                        String namePackage = packages.substring(startIdx, bracketIdx-1).trim();
                        System.out.println(namePackage);
                        int bracketIdxEnd = packages.indexOf(')', startIdx);
                        if (bracketIdxEnd == -1) {
                            break;
                        }                        
                        startIdx = bracketIdxEnd + 2;
                    }
                }
            }
      
            if (line.startsWith("End-Date:")) {
                installedByDateFilter = false;
            }
        }
    }

    /**
     * Main proc
     * @param args
     * @throws FileNotFoundException
     * @throws IOException 
     */
    public static void main(String[] args) throws FileNotFoundException, IOException {
        new InstalledPackages(args).run();
    }

}
