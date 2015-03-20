package com.fluidops.iwb.provider;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.SMO;
import weka.classifiers.trees.J48;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;
import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

public class PrepareDatabase {

	public static void main(String[] args) throws Exception{
		
		File input = new File("./tables.html");
		Document doc = null;
		Elements tables = null;

		try {
			
			prepareDB();
			
			DataSource source = new DataSource("/Users/mikhailgalkin/Documents/workspace/HTMLProvider/train.arff");
			Instances data = source.getDataSet();
			data.setClassIndex(data.numAttributes()-2);
			String[] options = new String[2];
			options[0]= "-R";
			options[1]="8";
			Remove remove = new Remove();
			remove.setOptions(options);
			remove.setInputFormat(data);
			Instances newData = Filter.useFilter(data, remove);
			//build model1 for the geniune/nongenuine prediction
			
			NaiveBayes bayes = new NaiveBayes();
			bayes.buildClassifier(newData);
			weka.core.SerializationHelper.write("/Users/mikhailgalkin/Documents/workspace/HTMLProvider/genuine.model", bayes);
			
			//build model for the orientation prediction
			NaiveBayes bayes2 = new NaiveBayes();
			data.setClassIndex(data.numAttributes()-1);
			bayes2.buildClassifier(data);
			weka.core.SerializationHelper.write("/Users/mikhailgalkin/Documents/workspace/HTMLProvider/orientation.model", bayes2);
			

		} catch (Exception e) {
			System.out.println("Error " + e.getLocalizedMessage());
		}
	
		//Deserialization
		//Classifier cls = (Classifier) weka.core.SerializationHelper.read("./tables.model");
		//System.out.println(cls);
	}
	
	public static Instances loadTrainData() throws Exception {
		DataSource source = new DataSource("./tables.arff");
		Instances data = source.getDataSet();
		System.out.println(source.getStructure());
		return data;
	}
	public static Classifier execute(Classifier cls, Instances data) throws Exception{
		cls.buildClassifier(data);
		return cls;
	}
	public static String evaluate(Classifier cls, Instances data, int folds, long seed) throws Exception{
		Evaluation eval = new Evaluation(data);
		eval.crossValidateModel(cls, data, folds, data.getRandomNumberGenerator(seed));
		return eval.toSummaryString();
	}
	
	public static void listFiles (final File folder){
		for (final File fileEntry:folder.listFiles()){
			if (fileEntry.isDirectory()){
				listFiles(fileEntry);
			} else {
				System.out.println(fileEntry.getName());
			}
				
		}
	}
	
	public static void prepareDB() throws Exception{
		DataSource source = new DataSource("/Users/mikhailgalkin/Documents/workspace/HTMLProvider/tables.arff");
		Instances structure = source.getStructure();
		Instances trainSet = new Instances(structure);
		StringService service = new StringService();

		final File folder = new File("/Users/mikhailgalkin/Downloads/tables");
		for (final File entry : folder.listFiles()) {
			System.out.println("Accessing file " + entry.getAbsolutePath());
			// create the reader for a particular csv file
			CSVReader csvReader = new CSVReader(new FileReader(
					entry.getAbsolutePath()));
			List<String[]> data = csvReader.readAll();
			
			double[] instanceValue = new double[structure.numAttributes()];
			// count diffMaxSimHorizontal

			try {
				instanceValue[0] = service.csv_diffMaxSimHorizontal(data);
			} catch (Exception e) {
				instanceValue[0] = 0.0;
			}
			// count diffMaxSimVertical
			try {
				instanceValue[1] = service.csv_diffMaxSimVertical(data);
			} catch (Exception e) {
				instanceValue[1] = 0.0;
			}
			// count diffAvgSimHorizontal
			try {
				instanceValue[2] = service.csv_diffAvgSimHorizontal(data);
			} catch (Exception e) {
				instanceValue[2] = 0.0;
			}
			// count diffAvgSimVertical
			try {
				instanceValue[3] = service.csv_diffAvgSimVertical(data);
			} catch (Exception e) {
				instanceValue[3] = 0.0;
			}
			instanceValue[4] = instanceValue[0] - instanceValue[1];
			instanceValue[5] = instanceValue[2] - instanceValue[3];
			if ((instanceValue[4] <0 && instanceValue[5]<0) ||(Math.abs(instanceValue[4])> Math.abs(instanceValue[5]))){
				instanceValue[7]=0; 
			} else 
				instanceValue[7]=1;
			
			if (instanceValue[0]==1.0 || instanceValue[1]==1.0){
				instanceValue[6]=1;
				instanceValue[7]=2;
			}
			trainSet.add(new DenseInstance(1.0, instanceValue));
			System.out.println(instanceValue[0]);
			System.out.println(instanceValue[1]);
			System.out.println(instanceValue[2]);
			System.out.println(instanceValue[3]);
			System.out.println(instanceValue[4]);
			System.out.println(instanceValue[5]);
			

			

		}
		// write the set to the file
		BufferedWriter writer = new BufferedWriter(new FileWriter(
				"/Users/mikhailgalkin/Documents/workspace/HTMLProvider/train.arff"));
		writer.write(trainSet.toString());
		writer.newLine();
		writer.flush();
		writer.close();
		
		//CSVWriter csv_writer = new CSVWriter(new FileWriter("/Users/mikhailgalkin/Documents/workspace/HTMLProvider/train.csv"));
		//csv_writer.writeNext(trainSet.toString().split(","));
		//csv_writer.close();
	}

	}


