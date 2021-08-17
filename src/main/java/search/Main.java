package search;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

public class Main {
	
	public static int COUNT_WORD_QUERY = 0;
	public static int UNIQUE_WORDS_IN_QUERY = 1;
	
	List<String> documents;
	List<String> stopwords;// we can skip this
	List<String> queries;
	
	
	public Main() throws IOException {
		InputStream is = Main.class.getResourceAsStream("stopwords.txt");
		
		documents = Arrays.asList(
				"Do you ever receive unexpected text messages out of the blue? Usually, these random messages are the product of a misdialed number or a lag in cell service, and other times, it’s spam from mailing lists and bots that floods your inbox with junk",
				"What could cause such a strange occurrence? As it turns out, the truth isn’t based in the supernatural. If you received one of these mysterious messages, don’t panic. There’s nothing wrong with your phone. Here’s why.",
				"After piecing the evidence together, Twitter users began speculating the messages weren’t related to a hack of any kind. Many of the messages dealt with themes of love, romance and intimacy. More importantly, several contained a message that revealed what was going on: “Happy Valentine’s Day.”",
				"After a short period of time, the country’s biggest carriers confirmed what everyone suspected. The messages everyone received were delayed, lost sometime around February 14, and were barely making it to phones across the nation.",
				"According to spokespeople from T-Mobile and Sprint, the glitch was caused by maintenance on a server that had been offline since, you guessed it, February 14. T-Mobile specifically attributed the issues to a third-party networking firm that provides routing services for several major carriers.",
				"When the server crashed on Valentine’s Day, any text messages en-route to their destination were trapped. Once the maintenance concluded and the server rebooted, all the delayed text messages were finally delivered."
			);		
		stopwords = IOUtils.readLines(is, StandardCharsets.UTF_8);
		documents = remove_stopword_documents(documents, stopwords);
		queries = Arrays.asList("supernatural", "mysterious", "message", "confirmed", "turns", "carriers", "cell", "phone");
		queries = remove_stopword_query(queries, stopwords);
	}
	
	private static List<String> remove_stopword_query(List<String> queries, List<String> stopwords) {
		List<String> res = new ArrayList<>();
		for(String q : queries) {
			if(!stopwords.contains(q)) {
				res.add(q.toLowerCase());
			}			
		}
		return res;
	}
	
	private static List<String> remove_stopword_documents(List<String> documents, List<String> stopwords) {
		List<String> docs = new ArrayList<>();
		for(String doc : documents) {
			docs.add(remove_stopword_document(doc, stopwords));
		}
		return docs;
	}
	
	private static String remove_stopword_document(String document, List<String> stopwords) {
		StringBuilder document_without_stopwords = new StringBuilder();
		String[] splitted = document.toLowerCase().split(StringUtils.SPACE);
		for(String word : splitted) {
			//todo remove special chars, get root word
			word = word.replaceAll("[.,;]+", StringUtils.EMPTY);			
			if(!stopwords.contains(word)) {
				document_without_stopwords.append(word).append(StringUtils.SPACE);
			}
		}
		return document_without_stopwords.toString().trim();
	}
	
	private static List<Object> getCountAndUniqueWordQuery(List<String> queries) {
		List<String> uniqueWordsInQuery = new ArrayList<>();
		Map<String, Integer> countWordQuery = new HashMap<>();
		for(String query : queries) {
			if(countWordQuery.containsKey(query)) {
				int total = countWordQuery.get(query);
				countWordQuery.replace(query, total + NumberUtils.INTEGER_ONE);
			} else {
				countWordQuery.put(query, NumberUtils.INTEGER_ONE);
				uniqueWordsInQuery.add(query);// could use stream distinct but this save time
			}
		}
		return Arrays.asList(countWordQuery, uniqueWordsInQuery);
	}
	
	private static List<Map<String, Integer>> setCountWordDocument(List<String> uniqueWordInQuery, List<String> documents) {
		int totalDocs = documents.size();
		List<Map<String, Integer>> countWordDocument = initCountWordDocument(uniqueWordInQuery, totalDocs); 
		for(int i = NumberUtils.INTEGER_ZERO; i < totalDocs; i++) {
			String doc = documents.get(i);
			Map<String, Integer> docMap = countWordDocument.get(i);
			for(String word : uniqueWordInQuery) {
				String[] splittedDoc = doc.split(StringUtils.SPACE);
				for(String spWord : splittedDoc) {
					if(StringUtils.equals(word, spWord)) {
						docMap.put(word, docMap.get(word) + 1);
					}
				}
			}
		}
		return countWordDocument;
	}
	
	private static List<Map<String, Integer>> initCountWordDocument(List<String> uniqueWordInQuery, int totalDocs) {
		List<Map<String, Integer>> list = new ArrayList<>(totalDocs);
		int total = totalDocs;
		while(total-- > 0) {
			list.add(initCountWordQuery(uniqueWordInQuery));
		}		
		return list;
	}
	
	private static Map<String, Integer> initCountWordQuery(List<String> uniqueWordInQuery) {
		Map<String, Integer> map = new HashMap<>();
		for(String word : uniqueWordInQuery) {
			map.put(word, NumberUtils.INTEGER_ZERO);
		}
		return map;
	}
	
	private static Map<String, Integer> countWordAllDoc(List<Map<String, Integer>> countWordDocument, List<String> uniqueWordsInQuery) {
		Map<String, Integer> map = initCountWordQuery(uniqueWordsInQuery);
		for(Map<String, Integer> wd : countWordDocument) {
			wd.keySet().forEach(key -> {
				map.put(key, map.get(key) + wd.get(key));
			});
		}
		return map;
	}
	
	private static double normalize(double score) {
		double probNotIn = NumberUtils.INTEGER_ONE - score;
		if(probNotIn < NumberUtils.INTEGER_ZERO) {
			return NumberUtils.INTEGER_ZERO;
		}
		if(probNotIn > NumberUtils.INTEGER_ONE) {
			return NumberUtils.INTEGER_ONE;
		}
		return probNotIn;
	}
	
	public static void main(String... args) throws IOException {
		Main main = new Main();
		// prepare data
		int totalDoc = main.documents.size();
		List<Object> countAndUniqueWordQuery = getCountAndUniqueWordQuery(main.queries);
		Map<String, Integer> countWordQuery = (Map<String, Integer>) countAndUniqueWordQuery.get(main.COUNT_WORD_QUERY);
		List<String> uniqueWordsInQuery = (List<String>) countAndUniqueWordQuery.get(main.UNIQUE_WORDS_IN_QUERY);
		List<Map<String, Integer>> countWordDocument = setCountWordDocument(uniqueWordsInQuery, main.documents);
		Map<String, Integer> countWordAllDoc = countWordAllDoc(countWordDocument, uniqueWordsInQuery);
		// calculate score
		List<Double> listScores = new ArrayList<>();
		for(int i = NumberUtils.INTEGER_ZERO; i < countWordDocument.size(); i++) {
			double score = NumberUtils.INTEGER_ZERO;
			for(String w : uniqueWordsInQuery) {
				score += countWordQuery.get(w) * countWordDocument.get(i).get(w) * Math.log(Math.log((totalDoc+1)/countWordAllDoc.get(w)));
			}
			listScores.add(normalize(score));
		}
		// print result
		listScores = listScores.stream().sorted().collect(Collectors.toList());
		for(int i = NumberUtils.INTEGER_ZERO; i < listScores.size(); i++)
		System.out.println("Doc " + i + " - " + listScores.get(i));
	}
}

