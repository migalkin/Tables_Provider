package com.fluidops.iwb.provider;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.Update;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.sail.memory.MemoryStore;

import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.TypeConfigDoc;
import com.fluidops.iwb.model.Vocabulary;
import com.fluidops.iwb.model.Vocabulary.DCTERMS;
import com.fluidops.iwb.provider.AbstractFlexProvider;
import com.fluidops.util.GenUtil;
import com.fluidops.util.StringUtil;

import org.jsoup.Jsoup;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.SMO;
import weka.classifiers.trees.J48;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

/**
 * Gathers data from a particular table from website and parses it into RDF
 * format
 * 
 * Current Provider URL (to be used as location):
 * http://gov.spb.ru/gov/otrasl/tr_infr_kom/tekobjekt/tek_rem/
 * 
 * @author mgalkin
 */
@TypeConfigDoc("Gathers data from a table on a website")
public class HTMLProvider extends AbstractFlexProvider<HTMLProvider.Config> {

	private static final Logger logger = Logger.getLogger(HTMLProvider.class
			.getName());
	private static final long serialVersionUID = 1000L;

	public static class Config implements Serializable {

		private static final long serialVersionUID = 1001L;

		@ParameterConfigDoc(desc = "URL of the source table", required = true)
		public String url;
	}

	@Override
	public void setLocation(String location) {
		config.url = location;
	}

	@Override
	public String getLocation() {
		return config.url;
	}

	@Override
	public void gather(List<Statement> res) throws Exception {

		String url = config.url;
		Document doc = Jsoup.connect(url).get();

		Elements tables = doc.select("table:not(:has(table))");
		Elements tableElem;
		URI nameURI = null;
		URI roadsURI = null;
		URI sideURI = null;
		URI totalURI = null;

		// Classifier genuine = (Classifier)
		// weka.core.SerializationHelper.read("/Users/mikhailgalkin/Documents/workspace/HTMLProvider/genuine.model");
		// Classifier orientation = (Classifier)
		// weka.core.SerializationHelper.read("/Users/mikhailgalkin/Documents/workspace/HTMLProvider/orientation.model");
		StringService service = new StringService();
		DataSource source = new DataSource(
				"/Users/mikhailgalkin/Documents/workspace/HTMLProvider/tables.arff");
		Instances data = source.getDataSet();
		Instances structure = source.getStructure();
		HTMLVocabulary vocab = new HTMLVocabulary(config.url.toString());

		for (Element table : tables) {
			Instances test = new Instances(structure);

			Elements rows = table.select("tr");
			double[] instanceValue = new double[structure.numAttributes()];

			// Count euristics
			instanceValue[0] = service.diffMaxSimHoriz(rows);
			if (Double.isNaN(instanceValue[0]))
				instanceValue[0] = 0.0;
			instanceValue[1] = service.diffMaxSimVertical(rows);
			if (Double.isNaN(instanceValue[1]))
				instanceValue[1] = 0.0;
			instanceValue[2] = service.diffAvgSimHoriz(rows);
			if (Double.isNaN(instanceValue[2]))
				instanceValue[2] = 0.0;
			instanceValue[3] = service.diffAvgSimVertical(rows);
			if (Double.isNaN(instanceValue[3]))
				instanceValue[3] = 0.0;
			instanceValue[4] = instanceValue[0] - instanceValue[1];
			instanceValue[5] = instanceValue[2] - instanceValue[3];
			test.add(new DenseInstance(1.0, instanceValue));

			System.out.println(rows.get(0).text());
			System.out.println(instanceValue[0]);
			System.out.println(instanceValue[1]);
			System.out.println(instanceValue[2]);
			System.out.println(instanceValue[3]);
			System.out.println(instanceValue[4]);
			System.out.println(instanceValue[5]);

			// predict values
			NaiveBayes bayes = new NaiveBayes();
			data.setClassIndex(data.numAttributes() - 2);
			test.setClassIndex(test.numAttributes() - 2);
			bayes.buildClassifier(data);
			// genuinity
			double gen_predict = bayes.classifyInstance(test.instance(0));
			String genuine_prediction = data.classAttribute().value(
					(int) gen_predict);
			System.out.println("Predicted value of instance with param "
					+ data.classAttribute().name() + " is "
					+ genuine_prediction);
			// orientaton
			data.setClassIndex(data.numAttributes() - 1);
			test.setClassIndex(test.numAttributes() - 1);
			bayes.buildClassifier(data);
			double or_predict = bayes.classifyInstance(test.instance(0));
			String orientation_predict = data.classAttribute().value(
					(int) or_predict);
			System.out.println("Predicted value of instance with param "
					+ data.classAttribute().name() + " is "
					+ orientation_predict);

			// write to the train.arff file
			
			// write to a database if valid
			if (genuine_prediction.equals("true")) {
				if (orientation_predict.equals("horizontal")) {
					nameURI = ProviderUtils.objectToURIInNamespace(
							vocab.HTML_CATALOG_NAMESPACE, "Name");
					roadsURI = ProviderUtils.objectToURIInNamespace(
							vocab.HTML_CATALOG_NAMESPACE, "roads_len");
					sideURI = ProviderUtils.objectToURIInNamespace(
							vocab.HTML_CATALOG_NAMESPACE, "side_len");
					totalURI = ProviderUtils.objectToURIInNamespace(
							vocab.HTML_CATALOG_NAMESPACE, "total");

					for (int i = 1; i < rows.size(); i++) {
						Elements column = rows.get(i).select("td");
						res.add(ProviderUtils.createStatement(ProviderUtils
								.objectToURIInNamespace(
										vocab.HTML_CATALOG_NAMESPACE,
										column.get(0).text()), RDF.TYPE,
								nameURI));
						res.add(ProviderUtils.createLiteralStatement(
								ProviderUtils.objectToURIInNamespace(
										vocab.HTML_CATALOG_NAMESPACE,
										column.get(0).text()), RDFS.LABEL,
								"Saint Petersburg " + column.get(0).text()));
						res.add(ProviderUtils.createLiteralStatement(
								ProviderUtils.objectToURIInNamespace(
										vocab.HTML_CATALOG_NAMESPACE,
										column.get(0).text()), roadsURI, column
										.get(1).text()));
						res.add(ProviderUtils.createLiteralStatement(
								ProviderUtils.objectToURIInNamespace(
										vocab.HTML_CATALOG_NAMESPACE,
										column.get(0).text()), sideURI, column
										.get(2).text()));
						res.add(ProviderUtils.createLiteralStatement(
								ProviderUtils.objectToURIInNamespace(
										vocab.HTML_CATALOG_NAMESPACE,
										column.get(0).text()), totalURI, column
										.get(3).text()));

					}

				}
			}
		}

		
	}

	@Override
	public Class<? extends Config> getConfigClass() {
		return HTMLProvider.Config.class;
	}

	private static void print(String msg, Object... args) {
		System.out.println(String.format(msg, args));
	}

	private static String trim(String s, int width) {
		if (s.length() > width)
			return s.substring(0, width - 1) + ".";
		else
			return s;
	}

	private class HTMLVocabulary {
		private String HTML_CATALOG_NAMESPACE;
		public final URI HTML_CATALOG;
		public final Literal HTML_CATALOG_LABEL;

		public HTMLVocabulary(String url) {
			HTML_CATALOG_NAMESPACE = url;

			// URI identifying the CKAN catalog itself
			HTML_CATALOG = ProviderUtils.objectToURIInNamespace(
					HTML_CATALOG_NAMESPACE, "datatable");

			// Label for the CKAN catalog
			HTML_CATALOG_LABEL = ProviderUtils.toLiteral(HTML_CATALOG_NAMESPACE
					+ "datatable");

		}

	}
}
