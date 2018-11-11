import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

public class trainAndTest1 {
	public static void main(String[] args) throws IOException {
		String func="ppr_embedding_wc";
		validate(func);
	}

	static void validate(String input) throws NumberFormatException, IOException{
		File file = new File("res/arxiv_trainout_"+input+".txt");
		BufferedReader reader = new BufferedReader(new FileReader(file));
		File file2 = new File("data/arxiv_adj_test.txt");
		BufferedReader reader2 = new BufferedReader(new FileReader(file2));
		File file3 = new File("data/arxiv_adj_train.txt");
		BufferedReader reader3 = new BufferedReader(new FileReader(file3));
		String line;
//		HashMap<Integer,Set<Integer>> es = new HashMap();
		HashMap<Integer,List<Integer>> rs = new HashMap();
//		while((line=reader3.readLine())!=null){
//			String[] words = line.split(" ");
//			int from = Integer.parseInt(words[0]);
//			Set<Integer> adj = es.get(from);
//			if(adj==null){
//				adj = new HashSet<Integer>();
//				es.put(from, adj);
//			}
//			for(int i=1;i<words.length;i++){
//				if(words[i].contains(":"))
//					words[i] = words[i].substring(0, words[i].indexOf(":"));
//				int to = Integer.parseInt(words[i]);
//				adj.add(to);
//				Set<Integer> adj2 = es.get(to);
//				if(adj2==null){
//					adj2 = new HashSet();
//					es.put(to, adj2);
//				}
//				adj2.add(from);
//			}
//		}
		while((line=reader.readLine())!=null){
			String[] words = line.split(" ");
			int from = Integer.parseInt(words[0]);
			rs.put(from, new ArrayList<Integer>());
			for(int i=1;i<words.length;i++){
				if(words[i].isEmpty())
					continue;
				if(words[i].contains(":"))
					words[i] = words[i].substring(0, words[i].indexOf(":"));
				int to = Integer.parseInt(words[i]);
				rs.get(from).add(to);
			}
		}
		int truth=0,hit=0,preds=0;
		while((line=reader2.readLine())!=null){
			String[] words = line.split(" ");
			int from = Integer.parseInt(words[0]);
			truth += words.length-1;
//			Set<Integer> adj = es.get(from);
//			if(!rs.containsKey(from))
//				continue;
			preds += rs.get(from).size();
			for(int i=1;i<words.length;i++){
				int to = Integer.parseInt(words[i]);
//				if(adj.contains(to)){
//					
//				}else
//					truth++;
				if(rs.get(from).contains(to))
					hit++;
			}
		}
		reader.close();
		reader2.close();
		//System.out.println(truth+" "+preds+" "+hit);
		System.out.println("truth="+(double)hit/truth+"; preds="+(double)hit/preds+" (truth:"+truth+" pred:"+preds+" hit:"+hit+")");
	}
}
