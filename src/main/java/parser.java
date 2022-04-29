/**
 * Parser for Jacoco document
 */

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.util.Objects;


public class parser {


    /**
     * return the value of a specific method parameter in the content
     * @param  path path to jacoco file
     */
    public static String getValue(String path, String path2) {
        try{
            File myFile = new File(path);
            if(!myFile.exists()){
                System.out.println("FILE NOT FOUND TRYING SECONDARY PATH");
                myFile = new File(path2);
                if(!myFile.exists()){
                    myFile = new File("/home/ren/iotdb/code-coverage/target/jacoco-merged-reports/index.html"); // hardcoded currently
                }
            }
            Document document = Jsoup.parse(myFile, "UTF-8");

            Elements elements = Objects.requireNonNull(document.body().getElementById("coveragetable")).select("#coveragetable > tfoot");
            StringBuilder x = new StringBuilder();

            for(Element e: elements){
                System.out.println(e.text());
                String[] information = e.text().split(" ");
                double instructionCoverage =1- Double.parseDouble(information[1].replace(",",""))/Double.parseDouble(information[3].replace(",",""));
                double branchCoverage = 1- Double.parseDouble(information[5].replace(",",""))/Double.parseDouble(information[7].replace(",",""));

                x.append(instructionCoverage).append(" ").append(branchCoverage);

            }
            System.out.println("Result " + x);
            return x.toString();

        } catch(IOException e){
                e.printStackTrace();
        }
        return "-1";
    }




    /**
     * Gets the overall results of the jacoco file
     * @return String array with the following results
     * [MissedInstructions,	Cov.,
     * Missed Branches Cov.
     * Missed Cxty,
     * Missed	Lines,
     * Missed	Methods,
     * Missed	Classes]
     */
    public static String getOverallResult(String path){
        String jacocoPath = path+"/target/site/jacoco-aggregate/index.html";
        String alternativePath = path +"/target/site/jacoco/index.html";
        return getValue(jacocoPath,alternativePath);
    }


    public static void main(String[] args){

    }
}
