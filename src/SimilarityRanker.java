import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public abstract class SimilarityRanker {
	String inputFile;
	int v_num=0;
	HashMap<Long, Set<Long>> graph;
	HashMap<Long, Integer> id2index;
	HashMap<Integer, Long> index2id;
	HashMap<Long, Long> degree = new HashMap();
	Set<Long> ids = new HashSet();
	long maxid=0;
//	int[][] graphM; 
//	double[][] matrix;
	HashMap<Long, Map<Long, Double>> scoreMap = new HashMap();
	
	public void readFromFile(String path) throws NumberFormatException, IOException {
		File file = new File(path);
		BufferedReader reader = new BufferedReader(new FileReader(file));
		graph = new HashMap<Long, Set<Long>>();
		String line;
		while ((line = reader.readLine()) != null) {
			String[] words = line.split(" ");
			long from = Long.parseLong(words[0]);
			maxid = maxid>from?maxid:from;
			ids.add(from);
			if(!graph.containsKey(from)){
				graph.put(from, new HashSet<Long>());
			}
			for (int i = 1; i < words.length; i++) {
				long to = Long.parseLong(words[i]);
				maxid = maxid>to?maxid:to;
				ids.add(to);
				graph.get(from).add(to);
				Set<Long> adjs = graph.get(to);
				if(adjs==null){
					adjs = new HashSet();
					graph.put(to, adjs);
				}
				adjs.add(from);
			}
		}
		int size = graph.size();
		System.out.println("graph size=" + size);
//		matrix = new double[size][size];
//		int cur= 0;
//		for(Entry<Long, Set<Long>> ent:graph.entrySet()){
//			long id = ent.getKey();
//			id2index.put(id, cur);
//			index2id.put(cur, id);
//			cur++;
//		}
//		for(Entry<Long, Set<Long>> ent:graph.entrySet()){
//			int fromIndex = id2index.get(ent.getKey());
//			for(Long toid:ent.getValue()){
//				int toIndex = id2index.get(toid);
//				graphM[fromIndex][toIndex] = 1;
//				graphM[toIndex][fromIndex] = 1;
//			}
//		}
	}
//	
//	public void generateSimilairyMatrix(){
//		for(Entry<Long, Map<Long, Double>> ent:scoreMap.entrySet()){
//			long id = ent.getKey();
//			int from = id2index.get(id);
//			for(Entry<Long,Double> ent1:ent.getValue().entrySet()){
//				int to= id2index.get(ent1.getKey());
//				matrix[from][to]=ent1.getValue();
//			}
//		}
//	}
	
	public abstract void generateSimilairyMap() throws IOException;
	
	class Pair{
		double score;
		int index;
		public Pair(int index, double score){
			this.score = score;
			this.index = index;
		}
	}
	
	public void outputAllInEdgeList(String path) throws IOException{
		BufferedWriter fw = new BufferedWriter(new FileWriter(path));
		for(Entry<Long, Map<Long, Double>> ent:scoreMap.entrySet()){
			Map<Long, Double> scorelist = ent.getValue();
			for(Entry<Long, Double> entt:scorelist.entrySet()){
				fw.write(ent.getKey()+"\t"+entt.getKey()+"\t"+entt.getValue()+"\r\n");
			}
		}
		fw.flush();
		fw.close();
	}
	
	public void generateTopk(String path, int k) throws IOException{
		BufferedWriter fw = new BufferedWriter(new FileWriter(path));
		for(Entry<Long, Map<Long, Double>> ent:scoreMap.entrySet()){
			Map<Long, Double> scorelist = ent.getValue();
			List<Entry<Long, Double>> res= new ArrayList();
			res.addAll(scorelist.entrySet());
			Collections.sort(res, new Comparator<Entry<Long, Double>>(){

				@Override
				public int compare(Entry<Long, Double> o1, Entry<Long, Double> o2) {
					double dif= o1.getValue()-o2.getValue();
					if(dif==0)
						return 0;
					else if(dif>0)
						return -1;
					else
						return 1;
				}
				
			});
			fw.write(ent.getKey()+" ");
			int cur=0;
			for(Entry<Long, Double> entt:res){
				if(cur++>=k)
					break;
				fw.write(entt.getKey()+" "); //+":"+p.score
			}
			fw.write("\r\n");
		}
//		for(int i=0; i<matrix.length; i++){
//			List<Pair> res = new ArrayList<Pair>();
//			for(int j=0; j<matrix.length; j++){
//				if(matrix[i][j]!=0)
//					res.add(new Pair(j, matrix[i][j]));
//			}
//			Collections.sort(res, new Comparator<Pair>(){
//
//				@Override
//				public int compare(Pair o1, Pair o2) {
//					return o1.score - o2.score >0 ? 1:-1;
//				}
//				
//			});
//			fw.write(index2id.get(i)+" ");
//			int cur=0;
//			for(Pair p:res){
//				if(cur++>=10)
//					break;
//				fw.write(index2id.get(p.index)+" "); //+":"+p.score
//			}
//			fw.write("\r\n");
//		}
	}
	
	public void saveScoreMap(String path) throws IOException{
		BufferedWriter fw = new BufferedWriter(new FileWriter(path));
		for(Entry<Long, Map<Long, Double>> ent:scoreMap.entrySet()){
			Map<Long, Double> scorelist = ent.getValue();
			List<Entry<Long, Double>> res= new ArrayList();
			res.addAll(scorelist.entrySet());
			fw.write(ent.getKey()+" ");
			int cur=0;
			for(Entry<Long, Double> entt:res){
				fw.write(entt.getKey()+":"+entt.getValue()+" "); //+
			}
			fw.write("\r\n");
		}
	}
}
