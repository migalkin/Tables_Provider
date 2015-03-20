package com.fluidops.iwb.provider;


import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import uk.ac.shef.wit.simmetrics.similaritymetrics.AbstractStringMetric;
import uk.ac.shef.wit.simmetrics.similaritymetrics.JaroWinkler;
import uk.ac.shef.wit.simmetrics.similaritymetrics.Levenshtein;
import uk.ac.shef.wit.simmetrics.similaritymetrics.QGramsDistance;

public class StringService {
	
	private String str1;
	private String str2;
	private Elements rows;
	private List<String[]> data;
	private static AbstractStringMetric metric = new Levenshtein();
	
	/*public String getStr1() {
		return str1;
	}
	public void setStr1(String str1) {
		this.str1 = str1;
	}
	public String getStr2() {
		return str2;
	}
	public void setStr2(String str2) {
		this.str2 = str2;
	}*/
	public Elements getRows() {
		return rows;
	}
	public void setRows(Elements rows) {
		this.rows = rows;
	}
	public List<String[]> getData() {
		return data;
	}
	public void setData(List<String[]> data) {
		this.data = data;
	}
	public static AbstractStringMetric getMetric() {
		return metric;
	}
	public static void setMetric(AbstractStringMetric metric) {
		StringService.metric = metric;
	}
	
	private static double modifiedLevenstein(String str1, String str2) {

		// Consider digits as the same objects
		try {
			String mod_str1 = str1.replace(",", ".");
			String mod_str2 = str2.replace(",", ".");
			double val1 = Double.parseDouble(mod_str1);
			double val2 = Double.parseDouble(mod_str2);
			mod_str1 = str1.replaceAll("[0-9]", "1");
			mod_str2 = str2.replaceAll("[0-9]", "1");
			return metric.getSimilarity(mod_str1, mod_str2);
		} catch (NumberFormatException e) {
			// Consider strings of more than 3 words as similar
			int threshold = 3;
			if (str1.split(" ").length >= threshold
					&& str2.split(" ").length >= threshold
					&& !str1.equals(str2)) {
				return 0.25;
			} else
				return metric.getSimilarity(str1, str2);
		}

	}
	
	public double diffMaxSimHoriz(Elements rows) {
		double max = 0;
		for (int k = 1; k < rows.size(); k++) {
			Elements cols = rows.get(k).select("td");
			double total = 0;
			for (int i = 0; i <= cols.size() - 1; i++) {
				for (int j = 0; j < cols.size(); j++) {

					String str1 = cols.get(i).text();
					String str2 = cols.get(j).text();
					// writer.println("Score between " + str1 + " and " + str2
					// + " is " + modifiedLevenstein(str1, str2));
					//total += modifiedLevenstein(str1, str2);
					total+=metric.getSimilarity(str1, str2);
				}
			}
			total = total / Math.pow(cols.size(), 2);
			if (total >= max)
				max = total;
			//System.out.println("total score is " + total);
		}
		return max;
	}
	
	public double diffMaxSimVertical(Elements rows) {
		int numCols = rows.get(0).select("td").size();

		// initializing
		ArrayList<Elements> cols = new ArrayList<Elements>();
		for (int i = 0; i < numCols - 1; i++) {
			Elements col = new Elements();
			cols.add(col);
		}
		// creating the columns
		for (Element row : rows) {
			Elements columns = row.select("td");
			for (int i = 1; i < numCols; i++) {
				Elements column = cols.get(i - 1);
				column.add(columns.get(i));
				cols.set(i - 1, column);
			}
		}
		// count
		double max = 0;
		for (int i = 0; i < cols.size(); i++) {
			Elements column = cols.get(i);
			double total = 0;
			for (int j = 0; j < column.size(); j++) {
				for (int k = 0; k < column.size(); k++) {
					String str1 = column.get(j).text();
					String str2 = column.get(k).text();
					//total += modifiedLevenstein(str1, str2);
					total+=metric.getSimilarity(str1, str2);
				}
			}
			total = total / Math.pow(column.size(), 2);
			if (total >= max)
				max = total;
		}
		return max;
	}
	
	public double diffAvgSimHoriz(Elements rows) {
		int numCols = rows.get(0).select("td").size();
		double very_total = 0;
		for (int k = 1; k < rows.size(); k++) {
			Elements cols = rows.get(k).select("td");
			double total = 0;
			for (int i = 0; i <= cols.size() - 1; i++) {
				for (int j = 0; j < cols.size(); j++) {

					String str1 = cols.get(i).text();
					String str2 = cols.get(j).text();
					
					//total += modifiedLevenstein(str1, str2);
					total+=metric.getSimilarity(str1, str2);
				}
			}
			total = total / Math.pow(cols.size(), 2);
			very_total += total;
			
		}
		very_total = very_total / (rows.size() - 1); // numCols
		return very_total;
	}
	
	public double diffAvgSimVertical(Elements rows) {
		int numCols = rows.get(0).select("td").size();

		// initializing
		ArrayList<Elements> cols = new ArrayList<Elements>();
		for (int i = 0; i < numCols - 1; i++) {
			Elements col = new Elements();
			cols.add(col);
		}
		// creating the columns
		for (Element row : rows) {
			Elements columns = row.select("td");
			for (int i = 1; i < numCols; i++) {
				Elements column = cols.get(i - 1);
				column.add(columns.get(i));
				cols.set(i - 1, column);
			}
		}
		// count
		double very_total = 0;
		for (int i = 0; i < cols.size(); i++) {
			Elements column = cols.get(i);
			double total = 0;
			for (int j = 0; j < column.size(); j++) {
				for (int k = 0; k < column.size(); k++) {
					String str1 = column.get(j).text();
					String str2 = column.get(k).text();
					//total += modifiedLevenstein(str1, str2);
					total+=metric.getSimilarity(str1, str2);
				}
			}
			total = total / Math.pow(column.size(), 2);
			very_total += total;
		}
		very_total = very_total / (numCols - 1); // *rows.size()
		return very_total;
	}
	
	public double csv_diffAvgSimVertical(List<String[]> data) {
		// TODO Auto-generated method stub
		int numCols = data.get(0).length;

		// initializing
		ArrayList<String[]> cols = new ArrayList<String[]>();
		for (int i = 0; i < numCols - 1; i++) {
			String[] col = new String[data.size()];
			cols.add(col);
		}
		// creating the columns
		for (int k = 0; k < data.size(); k++) {
			String[] columns = data.get(k);
			for (int i = 1; i < numCols; i++) {
				String[] column = cols.get(i - 1);
				column[k] = columns[i];
				cols.set(i - 1, column);
			}
		}
		// count
		double very_total = 0;
		for (int i = 0; i < cols.size(); i++) {
			String[] column = cols.get(i);
			double total = 0;
			for (int j = 0; j < column.length; j++) {
				for (int k = 0; k < column.length; k++) {
					String str1 = column[j];
					String str2 = column[k];
					//total += modifiedLevenstein(str1, str2);
					total+=metric.getSimilarity(str1, str2);
				}
			}
			total = total / Math.pow(column.length, 2);
			very_total += total;
		}
		very_total = very_total / (numCols - 1); // *rows.size()
		return very_total;
	}

	public double csv_diffAvgSimHorizontal(List<String[]> data) {
		// TODO Auto-generated method stub
		int numCols = data.get(0).length;
		double very_total = 0;
		for (int k = 1; k < data.size(); k++) {
			String[] cols = data.get(k);
			double total = 0;
			for (int i = 0; i <= cols.length - 1; i++) {
				for (int j = 0; j < cols.length; j++) {

					String str1 = cols[i];
					String str2 = cols[j];
					// System.out.println("Score between " + str1 + " and "
					// + str2 + " is "
					// + metric.getSimilarity(str1, str2));
					//total += modifiedLevenstein(str1, str2);
					total+=metric.getSimilarity(str1, str2);
				}
			}
			total = total / Math.pow(cols.length, 2);
			very_total += total;
			System.out.println("used metric "+metric.getShortDescriptionString() );
		}
		very_total = very_total / (data.size() - 1); // numCols
		return very_total;
	}

	public double csv_diffMaxSimVertical(List<String[]> data) {
		// TODO Auto-generated method stub
		int numCols = data.get(0).length;

		// initializing
		ArrayList<String[]> cols = new ArrayList<String[]>();
		for (int i = 0; i < numCols - 1; i++) {
			String[] col = new String[data.size()];
			cols.add(col);
		}
		// creating the columns
		for (int k = 0; k < data.size(); k++) {
			String[] columns = data.get(k);
			for (int i = 1; i < numCols; i++) {
				String[] column = cols.get(i - 1);
				column[k] = columns[i];
				cols.set(i - 1, column);
			}
		}
		// count
		double max = 0;
		for (int i = 0; i < cols.size(); i++) {
			String[] column = cols.get(i);
			double total = 0;
			for (int j = 0; j < column.length; j++) {
				for (int k = 0; k < column.length; k++) {
					String str1 = column[j];
					String str2 = column[k];
					//total += modifiedLevenstein(str1, str2);
					total+=metric.getSimilarity(str1, str2);
				}
			}
			total = total / Math.pow(column.length, 2);
			if (total >= max)
				max = total;
		}
		return max;
	}

	public double csv_diffMaxSimHorizontal(List<String[]> data) {
		// TODO Auto-generated method stub
		double max = 0;
		for (int k = 1; k < data.size(); k++) {
			// Elements cols = rows.get(k).select("td");
			String[] cols = data.get(k);
			double total = 0;
			for (int i = 0; i <= cols.length - 1; i++) {
				for (int j = 0; j < cols.length; j++) {

					String str1 = cols[i];
					String str2 = cols[j];
					// writer.println("Score between " + str1 + " and " + str2
					// + " is " + modifiedLevenstein(str1, str2));
					//total += modifiedLevenstein(str1, str2);
					total+=metric.getSimilarity(str1, str2);
				}
			}
			total = total / Math.pow(cols.length, 2);
			if (total >= max)
				max = total;
			//System.out.println("total score is " + total);
		}
		return max;
	}
	

}
