package org.jcryptool.analysis.fleissner.logic;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.io.FileNotFoundException;
import java.math.BigInteger;
import java.time.Duration;

import org.jcryptool.analysis.fleissner.Activator;
import org.jcryptool.core.logging.utils.LogUtil;


/**
 * @author Dinah
 *
 */
public class MethodApplication{
	
    private ArrayList<Integer> possibleTemplateLengths = new ArrayList<>();

//  parameters given by user or default
    private String method, decryptedText, encryptedText, textInLine, language;
    private int templateLength, holes, nGramSize;
    private int[] grille;
    private BigInteger restart, tries, sub;
    private boolean isPlaintext;
    private CryptedText ct;
    private FleissnerGrille fg;
    private TextValuator tv;
    private long start, end;
    private double statistics[];
	
//	parameters for analysis
    private double value, oldValue, alltimeLow=Double.MAX_VALUE;
    private int changes=0, iAll=0, grilleNumber=0, improvement = 0; 
    private int x,y,move;
    private int[] bestTemplate=null;
    private String lastImprovement = null, bestDecryptedText = "", procedure = "";
	
    private String fwAnalysisOutput;
	
    /**
     * applies parameter settings from ParameterSettings and sets and executes method
     * 
     * @param ps object parameter settings 
     * @param analysisOutput the text field in FleissnerWindow where analysis progress is displayed
     * @param argStatistics
     * @throws FileNotFoundException
     */
	public MethodApplication(ParameterSettings ps, double argStatistics[]) throws FileNotFoundException {
        
	    this.fwAnalysisOutput = new String("");
        this.method = ps.getMethod();
        this.textInLine = ps.getTextInLine();
        this.templateLength = ps.getTemplateLength();
        if (!ps.getPossibleTemplateLengths().isEmpty()) {
            this.possibleTemplateLengths = ps.getPossibleTemplateLengths();
        }
        this.isPlaintext = ps.isPlaintext();
        this.holes = ps.getHoles();
        this.grille = ps.getGrille();
        this.restart = ps.getRestart(); 
        this.language = ps.getLanguage();
        this.statistics = argStatistics;
        this.nGramSize = ps.getnGramSize();
        this.ct = new CryptedText();
        this.fg = new FleissnerGrille(templateLength);
		if (method.equals("analyze")) {
			try {
				this.tv = new TextValuator(statistics, language, nGramSize);
			} catch (FileNotFoundException e) {
				LogUtil.logError(Activator.PLUGIN_ID, "Statistikdatei konnte nicht gefunden werden", e, true);
				throw new FileNotFoundException("Datei nicht gefunden!");
			}
		}
	}		
	
	/**
	 * Executes one of the methods Brute-Force or Hill-Climbing dependent on templateLength
	 */
	public void analyze () {

	    start = System.currentTimeMillis();
	    fwAnalysisOutput += "\nStart analysis";
	    LogUtil.logInfo(Activator.PLUGIN_ID, "Start analysis");
	    
	    if (this.possibleTemplateLengths.isEmpty()) {
	    	
			ct.load(textInLine, isPlaintext, templateLength, grille, fg);
			fwAnalysisOutput += "\nInfo:"+ct.toString();
			LogUtil.logInfo(Activator.PLUGIN_ID, "\nINFO:\n"+ct.toString());
			if (templateLength<5){
//				Brute Force for up to 4 x 4 grilles. Creates all possibles grilles and evaluates each text decrypted by them
				procedure = "Brute-Force";
				fwAnalysisOutput += "\nStart "+procedure;
				LogUtil.logInfo(Activator.PLUGIN_ID, "Start "+procedure);
				this.bruteForce();
				fwAnalysisOutput += "\nFinished "+procedure;
				LogUtil.logInfo(Activator.PLUGIN_ID, "\nFinished "+procedure);
			}
			else {
//				Hill-Cimbing for templates from 5 x 5. creates random grilles and evaluates those and slightly variations of them before
//				trying new random grilles.
				procedure = "Hill-Climbing";
				fwAnalysisOutput += "\nStart "+procedure;
				LogUtil.logInfo(Activator.PLUGIN_ID, "Start "+procedure);
				this.hillClimbing();
				fwAnalysisOutput += "\nFinished "+procedure;
				LogUtil.logInfo(Activator.PLUGIN_ID, "\nFinished "+procedure);
			}
	    }else {
//          if no key size is given, key size will be narrowed down to the only possible ones and those will be analyzed
	    	double tempvalue = Double.MAX_VALUE, relativeValue, tempRelativeValue = Double.MAX_VALUE;
	    	int[] tempTemplate=null;
	    	int tempLength=0, tempTry=0;
	    	String tempProcedure=null, tempLastImprovement=null;
	    	
	    	for (int length : this.possibleTemplateLengths) {
	    		this.templateLength = length;
	    		fwAnalysisOutput += "\nTry with length: "+templateLength;
	    		LogUtil.logInfo(Activator.PLUGIN_ID, "Try with length: "+templateLength);
				if (templateLength%2==0) {
					this.holes = (int) (Math.pow(templateLength, 2))/4;
				}
				else {
					this.holes = (int) (Math.pow(templateLength, 2)-1)/4;
				}
				this.fg = new FleissnerGrille(templateLength);
				this.ct = new CryptedText();
				this.grille = null;
				this.isPlaintext = false;
				ct.load(textInLine, isPlaintext, templateLength, grille, fg);
				fwAnalysisOutput += "\nINFO:\n"+ct.toString();
				LogUtil.logInfo(Activator.PLUGIN_ID, "INFO:\n"+ct.toString());
	    		if (templateLength<5){
//	    			Brute Force for up to 4 x 4 grilles. Creates all possibles grilles and evaluates each text decrypted by them
	    			procedure = "Brute-Force";
	    			fwAnalysisOutput += "\nStart "+procedure;
	    			LogUtil.logInfo(Activator.PLUGIN_ID, "Start "+procedure);
	    			this.bruteForce();
	    			fwAnalysisOutput += "\nFinished "+procedure;
	    			LogUtil.logInfo(Activator.PLUGIN_ID, "\nFinished "+procedure);
	    		}
	    		else {
//	    			Hill-Cimbing for templates from 5 x 5. creates random grilles and evaluates those and slightly variations of them before
//	    			trying new random grilles.
	    			procedure = "Hill-Climbing";
	    			fwAnalysisOutput += "\nStart "+procedure;
	    			LogUtil.logInfo(Activator.PLUGIN_ID, "Start "+procedure);
	    			this.hillClimbing();
	    			fwAnalysisOutput += "\nFinished "+procedure;
	    			LogUtil.logInfo(Activator.PLUGIN_ID, "\nFinished "+procedure);
	    		}
//	    		sets value in relation to size
	    		relativeValue = alltimeLow/fg.decryptText(ct.getText()).length();
	    		if (relativeValue<tempRelativeValue) {
	    		    fwAnalysisOutput += "\nvalue has been changed. Relative value is: "+relativeValue+" relative to "+alltimeLow+" with length "+length;
	    			LogUtil.logInfo(Activator.PLUGIN_ID, "value has been changed. Relative value is: "+relativeValue+" relative to "+alltimeLow+" with length "+length);
	    			tempRelativeValue = relativeValue;
	    			tempvalue = alltimeLow;
	    			tempTemplate = bestTemplate;
	    			tempLength = templateLength;
	    			tempLastImprovement = lastImprovement;
	    			tempProcedure = procedure;
	    			tempTry = grilleNumber;
	    		}
	    		alltimeLow=Double.MAX_VALUE;
	    		changes=0;
	    		iAll=0;
	    		grilleNumber=0;
	    		improvement = 0;
	    		bestTemplate=null;
	    		lastImprovement = null;
	    		bestDecryptedText = "";
	    		procedure = "";
	    	}
			alltimeLow = tempvalue;
			bestTemplate = tempTemplate;
			this.templateLength = tempLength;
			lastImprovement = tempLastImprovement;
			procedure = tempProcedure;
			grilleNumber = tempTry;
			
			this.fg = new FleissnerGrille(templateLength);
			this.ct = new CryptedText();
			ct.load(textInLine, isPlaintext, tempLength, grille, fg);
			fg.useTemplate(bestTemplate, templateLength);
	    }
		end = System.currentTimeMillis() - start; 
	}
	
	/**
	 * bruteForce() method tries every possible grille with current key size and chooses the one that generates the best text value
	 */
	public void bruteForce() {

//		clears grilles in FleissnerGrille from potential earlier encryptions before building new ones
		fg.clearGrille();
		ArrayList<int[]> templateList = fg.bruteForce(templateLength, holes);
		if (templateList.isEmpty()) {
		    fwAnalysisOutput += "\ntemplateList is empty";
			LogUtil.logInfo(Activator.PLUGIN_ID, "templateList is empty");
			return;
		}
		fwAnalysisOutput += "\nsift through all templates";
		LogUtil.logInfo(Activator.PLUGIN_ID, "run through all templates");
		// using every possible template / Grille
		for (int[] template : templateList) {
				
			iAll++;
			fg.useTemplate(template, templateLength);
			decryptedText = fg.decryptText(ct.getText());
			value = tv.evaluate(decryptedText);
			if (value < alltimeLow)
			{
//				better grille found, saves new template
				alltimeLow = value;
				bestTemplate = template;
				grilleNumber = iAll;
			}	
            
            fwAnalysisOutput += "\n\nGrille: "+iAll+fg;
            fwAnalysisOutput += "\nAccurateness: " + value + " (best: "+alltimeLow+")";
            fwAnalysisOutput += "\nDecrypted text:\n ==> "+decryptedText+"\n";

			LogUtil.logInfo(Activator.PLUGIN_ID, "\n\nGrille: "+iAll+fg);
			LogUtil.logInfo(Activator.PLUGIN_ID, "Accurateness: " + value + " (best: "+alltimeLow+")");
			LogUtil.logInfo(Activator.PLUGIN_ID, "Decrypted text:\n ==> "+decryptedText+"\n");
		}
		fg.useTemplate(bestTemplate, templateLength);	
	}
	
	/**
	 * hillClimbing method is the analysis method for grilles of size 5 and higher. It creates random grilles in respective size
	 * and makes step by step changes to improve text value. If local maximum is reached a new random grille will be created.
	 * This process goes on until the number of 'restarts' is reached and the grille that created the best valued text will be
	 * chosen as the right one.
	 */
	public void hillClimbing() {

		tries = restart;
		sub = BigInteger.valueOf(1);
		
		do {
//			clears grilles in FleissnerGrille from potential earlier encryptions before building a random new one
			fg.clearGrille();
//			start with highest possible value so process will minimize value from the beginning
			oldValue = Double.MAX_VALUE;
			double min;
			int minX=0, minY=0, minMove=0;
//			create random grille
			for(int i=0; i<holes; i++)
			{
				do
				{
					x = ThreadLocalRandom.current().nextInt(0, templateLength);
					y = ThreadLocalRandom.current().nextInt(0, templateLength);
				}
				while (!fg.isPossible(x, y));

			fg.setState(x, y, true);
			}
			fwAnalysisOutput += "\n\nRestart: "+sub;
			LogUtil.logInfo(Activator.PLUGIN_ID, "\n\nRestart: "+sub);
			decryptedText = fg.decryptText(ct.getText());
			min = tv.evaluate(decryptedText);
			fwAnalysisOutput += "\nDecrypted Text: \n"+decryptedText+"\n\nwith value: "+String.valueOf(min);
			LogUtil.logInfo(Activator.PLUGIN_ID, "Decrypted Text: "+decryptedText+"with value: "+String.valueOf(min));
			
			do{
				iAll++;
				// calculate the best possible solution if only changing one of the cells
				for (x=0; x<templateLength; x++)
				{
					for (y=0; y<templateLength; y++)
					{
						if (fg.isFilled(x, y))
						{
							for (move=1; move<=3; move++)
							{
//							    try other 3 positions of every hole in grille
								fg.change(x, y, move);
								decryptedText = fg.decryptText(ct.getText());
								value = tv.evaluate(decryptedText);
								fg.undoChange(x, y, move);
								
								if (value < min)
								{
									min = value;
									minX = x;
									minY = y;
									minMove = move;
								}	
							}
						}
					}
				}
				
				if (min < oldValue)
				{
					// we found a better solution by changing one of the cells
					// go on with this new solution
					fg.change(minX, minY, minMove);
					oldValue = min;
					decryptedText = fg.decryptText(ct.getText());
					improvement++;
					if (oldValue<alltimeLow) 
					{
						alltimeLow=oldValue;
						grilleNumber=iAll;
						bestDecryptedText = fg.decryptText(ct.getText());
						bestTemplate = fg.saveTemplate(holes);
						lastImprovement = String.valueOf(sub);
						changes++;
						fwAnalysisOutput += "\n\nbest grille yet with "+changes+" changes, where value is " +alltimeLow+"\n"+fg+"\n";
						LogUtil.logInfo(Activator.PLUGIN_ID, "best grille yet with "+changes+" changes, where value is " +alltimeLow+"\n"+fg);
					} 
                    fwAnalysisOutput += "\n\ntry: " + iAll + ", changes: "+changes + " (last at: " + grilleNumber + " in restart: "+lastImprovement+"), accurateness: " + min + " (best: "+oldValue+", alltime: "+alltimeLow+")\n";
                    fwAnalysisOutput += "\n==> "+decryptedText+"\n\n Grille: "+fg+"\n";
					LogUtil.logInfo(Activator.PLUGIN_ID, "try: " + iAll + ", changes: "+changes + " (last at: " + grilleNumber + " in restart: "+lastImprovement+"), accurateness: " + min + " (best: "+oldValue+", alltime: "+alltimeLow+")");
					LogUtil.logInfo(Activator.PLUGIN_ID, "==> "+decryptedText+"\n Grille: \n"+fg);
				}
			} while (Math.abs(iAll-improvement)<1);
			tries = restart.subtract(sub);
			sub = sub.add(BigInteger.valueOf(1));
			improvement = 0;
			iAll = 0;
//			start next restart
			
		}while(tries.compareTo(BigInteger.valueOf(0))==1);
		
		fg.useTemplate(bestTemplate, templateLength);
		int rotMove = 0;
//		checks all 4 rotation positions of the found grille
		for (move=1; move<=4; move++)
		{
			fg.rotate();
			decryptedText = fg.decryptText(ct.getText());
			value = tv.evaluate(decryptedText);
						
			if (value < alltimeLow)
			{
				alltimeLow = value;
				rotMove = move;
				improvement++;
			}							
		}
		if (improvement != 0) {
					
			for (int moves = 1 ; moves <= rotMove; moves++) {
				fg.rotate();
			}
			bestTemplate = fg.saveTemplate(holes);
		}
	}
	
	/**
	 * encrypts given plaintext with given key directly through load method of class CryptedText
	 */
	public void encrypt(){

		ct.load(textInLine, isPlaintext, templateLength, grille, fg);
		encryptedText = "";
		for(char[][]textPart:ct.getText()) {
			for (int y = 0; y < templateLength; y++) {
				for (int x = 0; x < templateLength; x++) {
					encryptedText += textPart[x][y];
				}
			}
		}
		LogUtil.logInfo(Activator.PLUGIN_ID, "Crypted text succesfully encrypted");
	}
	
	/**
	 * decrypts given crypted text with given key by decryption method of class FleissnerGrille
	 */
	public void decrypt(){

		ct.load(textInLine, isPlaintext, templateLength, grille, fg);
		fg.useTemplate(grille, templateLength);
		decryptedText = fg.decryptText(ct.getText());
		LogUtil.logInfo(Activator.PLUGIN_ID, "Crypted text succesfully decrypted");
	}
	
	/**
	 * generates random key and saves it as int array 'grille'
	 */
	public void keyGenerator() {
		this.grille = new int[2*holes];	
		fg.clearGrille();
		
		for(int i=0; i<holes; i++)
		{
			do
			{
				x = ThreadLocalRandom.current().nextInt(0, templateLength);
				y = ThreadLocalRandom.current().nextInt(0, templateLength);
			}
			while (!fg.isPossible(x, y));

			fg.setState(x, y, true);
			grille[2*i] = x;
			grille[(2*i)+1] = y;
		}
	}
	
	/**
	 * @return the fg
	 */
	public FleissnerGrille getFg() {
		return fg;
	}

	/**
	 * @param fg the fg to set
	 */
	public void setFg(FleissnerGrille fg) {
		this.fg = fg;
	}

	public String getEncryptedText() {
        return encryptedText;
    }

    public String getDecryptedText() {
        return decryptedText;
    }
    public String getBestDecryptedText() {
        return bestDecryptedText;
    }

    public int[] getBestTemplate() {
        return bestTemplate;
    }

    /**
     * @return the fwAnalysisOutput
     */
    public String getFwAnalysisOutput() {
        return fwAnalysisOutput;
    }

    /**
     * @param fwAnalysisOutput the fwAnalysisOutput to set
     */
    public void setFwAnalysisOutput(String fwAnalysisOutput) {
        this.fwAnalysisOutput = fwAnalysisOutput;
    }

    @Override
    public String toString() {
		
		String output= null;
		String bestTemplateCoordinates = "";
		
		switch(this.method) {
		
		case "analyze":
		    String time;
			bestDecryptedText = fg.decryptText(ct.getText());
			value = tv.evaluate(bestDecryptedText);
			for (int i = 0; i<bestTemplate.length;i++) {
				bestTemplateCoordinates+=bestTemplate[i];
			}
			output = "\nBest grille: "+bestTemplateCoordinates+"\n"+fg+"\nwith length: "+templateLength+" found at try: "+grilleNumber+", in Restart: "+lastImprovement;
			output += "\nDecrypted text:\n\n"+bestDecryptedText+"\n\n";
//			adjusts time format depending of spent time for analysis
			if (end<60000)
			    time = end+" milliseconds";
			else {
			    long timeInDays, timeInHours, timeInMinutes, timeInSeconds;
			    
			    timeInSeconds = end/1000;
			    Duration duration = Duration.ofSeconds(timeInSeconds);

			    timeInDays = duration.toDays();
			    duration=duration.minusDays(timeInDays);
			    timeInHours = duration.toHours();
			    duration=duration.minusHours(timeInHours);
			    timeInMinutes = duration.toMinutes();
			    duration=duration.minusMinutes(timeInMinutes);
			    timeInSeconds = duration.getSeconds();
	            time = String.format("%02d:%02d:%02d:%02d", timeInDays, timeInHours, timeInMinutes, timeInSeconds)+" (dd:hh:mm:ss)";
			}
			output += "Finished analysis in "+time;
			break;
		case "encrypt":
			output = "\nEncrypted text:\n\n"+encryptedText+",\nencrypted with key:"+fg;
			for (int i = 0; i<grille.length;i++) {
				bestTemplateCoordinates+=grille[i];
			}
			output += "\nKey coordinates: "+bestTemplateCoordinates;
			output += "\nwith length: "+templateLength;
			break;
		case "decrypt":
			output = "\nDecrypted text:\n\n"+decryptedText+", decryption with key:"+fg;
			for (int i = 0; i<grille.length;i++) {
				bestTemplateCoordinates+=grille[i];
			}
			output += "\nKey coordinates: "+bestTemplateCoordinates;
			output += "\nwith length: "+templateLength;
			break;
		case "keyGenerator":
			output = "Key: "+fg;
			for (int h=0;h<grille.length;h++) {
				output += grille[h]+",";
			}
			output += "\nwith length: "+templateLength;
			fg.clearGrille();
		}
		return output;
	}
}
