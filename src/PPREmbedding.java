import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;


public class PPREmbedding extends SimilarityRanker {
	double[][] w;
	double[][] con;
	
	double[][] w_last;
	double[][] con_last;
	/**
	 * parameters
	 */
	public static int layer_size = 64;
	public static double alpha = 0.0025f;
	public static double starting_alpha = 0.05f;
	public static double jump_factor = 0.15f;
	public static int MAX_EXP = 5;
	public static Random r = new Random(0);
	// use to calculate e^i quickly
	public static double[] expTable;
	public static long next_random;
	public static int magic = 100;
	// negative samples
	public static int neg = 5;

	// #sampled paths-one hop
	public static int step = 10;
	public static int SAMPLE = 200;

	static {
		expTable = new double[1000];
		for (int i = 0; i < 1000; i++) {
			expTable[i] = (double) Math.exp((i / (double) 1000 * 2 - 1)
					* MAX_EXP); // Precompute the exp() table
			expTable[i] = expTable[i] / (expTable[i] + 1); // Precompute f(x) =
															// x / (x + 1)
		}
	}

	public static void rand_init(double[][] w) {
		for (int i = 0; i < w.length; i++) {
			double[] tmp = w[i];
			for (int j = 0; j < tmp.length; j++) {
				tmp[j] = (r.nextDouble() - 0.5) / layer_size;
			}
		}
	}
	static double global = 0;
	int size = 0;
	public void generateSimilairyMap() throws IOException {
		size = (int) Math.max(maxid + 1, ids.size());
		System.out.println(size);
		w = new double[size][layer_size];
		con = new double[size][layer_size];
		w_last = new double[size][layer_size];
		con_last = new double[size][layer_size];
		rand_init(w);
		// rand_init(con);

		Map<Long, List<Long>> g = new HashMap<Long, List<Long>>();

		for (Entry<Long, Set<Long>> ent : graph.entrySet()) {
			g.put(ent.getKey(), new ArrayList<Long>(ent.getValue()));
		}
	
		int iter = 200;
		alpha = starting_alpha;
		for (int kk = 0; kk < iter; kk++) {
			global = 0;
//			alpha = Math.max(0.0001, starting_alpha * (iter-kk)/iter);
			for (int root = 0; root < size; root++) {
//				if (root % 1000 == 1) {
//					alpha = alpha * (1 - root * 1.0 / size);
//					if (alpha < alpha * 0.0001) {
//						alpha = alpha * 0.0001;
//					}
//				}
				List<Long> adjs = g.get((long) root);
				if (adjs == null || adjs.size() == 0)
					continue;
				for (int i = 0; i < SAMPLE; i++) {
					// sampled: from a to b
					int s = step;
					long id = -1;
					List<Long> tmp_adj = adjs;
					while (s-- > 0) {
						double jump = r.nextDouble();
						if (jump < jump_factor) {
							break;
						} else {
							id = tmp_adj.get(r.nextInt(tmp_adj.size()));
							tmp_adj = g.get((long) id);
						}
					}
					if (id != -1) {
						double weight = g.get((long) id).size() * 1.0 / magic;
						double[] e = new double[layer_size];
						double[] e1 = new double[layer_size];
						// update as :word a, context b
						updateVector(w[root], con[(int) id], 1, weight, e);

						for (int j = 0; j < neg; j++) {
							int nid = r.nextInt(size);
							if (nid == root)
								continue;
							List<Long> adj = g.get((long) nid);
							weight = 0;
							if (adj != null && !adj.isEmpty())
								weight = g.get((long) nid).size() * 1.0 / magic;
							else
								continue;
							updateVector(w[root], con[(int) nid], 0, weight, e);
						}

						for (int k = 0; k < layer_size; k++)
							w[root][k] += e[k];

//						 update as :word b, context a
//						updateVector(w[(int) id], con[root], 1, weight, e1);
//
//						for (int j = 0; j < neg; j++) {
//							int nid = r.nextInt(size);
//							if (nid == root)
//								continue;
//							List<Long> adj = g.get((long) nid);
//							weight = 0;
//							if (adj != null && !adj.isEmpty())
//								weight = g.get((long) nid).size() * 1.0 / magic;
//							else
//								continue;
//							updateVector(w[(int) id], con[(int) nid], 0,
//									weight, e1);
//						}
//
//						for (int k = 0; k < layer_size; k++)
//							w[(int) id][k] += e1[k];
					}

				}
			}
			System.out.println("iter:"+kk+":likelihood:"+global);
		}
	}

	private void updateVector(double[] w, double[] c, int label, double weight,
			double[] e) {
		double neg_g = calculateGradient(label, w, c, weight);
		for (int i = 0; i < w.length; i++) {
			double tmp_c = c[i];
			double tmp_w = w[i];
			e[i] += neg_g * tmp_c;
			c[i] += neg_g * tmp_w;
		}
	}

	private static double calculateGradient(int label, double[] w, double[] c,
			double weight) {
		double f = 0, g;
		for (int i = 0; i < layer_size; i++)
			f += w[i] * c[i];
		if (f > MAX_EXP)
			g = (label - 1) * alpha;
		else if (f < -MAX_EXP)
			g = (label - 0) * alpha;
		else{
			double sigmoid = expTable[(int) ((f + MAX_EXP) * (1000 / MAX_EXP / 2))];
			g = (label - sigmoid)
					* alpha;
			if(label==1){
				global +=  Math.log(sigmoid);
			}else
				global +=  Math.log(1-sigmoid);
		}
		return g;
	}
	
	public void generateTopk(String path, int k) throws IOException {
		BufferedWriter fw_ww = new BufferedWriter(new FileWriter(path+"_ww.txt"));
		BufferedWriter fw_wc = new BufferedWriter(new FileWriter(path+"_wc.txt"));
		BufferedWriter fw_vec = new BufferedWriter(
				new FileWriter(path + "_vec.txt"));
		for (int i = 0; i < w.length; i++) {
			fw_vec.write(i + ": ");
			for (int j = 0; j < layer_size; j++) {
				fw_vec.write(w[i][j] + " ");
			}
			fw_vec.write("\r\n");
		}
		fw_vec.flush();
		fw_vec.close();
		
		fw_vec = new BufferedWriter(
				new FileWriter(path + "_vec_con"));
		for (int i = 0; i < con.length; i++) {
			fw_vec.write(i + ": ");
			for (int j = 0; j < layer_size; j++) {
				fw_vec.write(con[i][j] + " ");
			}
			fw_vec.write("\r\n");
		}
		fw_vec.flush();
		fw_vec.close();
		
		fw_vec = new BufferedWriter(new FileWriter(path+"_score"));
		for (int i = 0; i < w.length; i++) {
			List<Pair> res_wc = new ArrayList();
			List<Pair> res_ww = new ArrayList();
			double[] vi = w[i];
			double sum = 0;
			for (int j = 0; j < w.length; j++) {
				if(i==j)
					continue;
				double[] vj = w[j];
				double sim = 0;
				for (int kk = 0; kk < vi.length; kk++)
					sim += vi[kk] * vj[kk];
				res_ww.add(new Pair(j, sim));
				double ppr1 = 0;
				for (int kk = 0; kk < vi.length; kk++)
					ppr1 += vi[kk] * con[j][kk];
				res_wc.add(new Pair(j, ppr1));
//				sum+=Math.exp(ppr1);
			}
			Collections.sort(res_wc, new Comparator<Pair>() {

				@Override
				public int compare(Pair o1, Pair o2) {
					double dif = o1.score - o2.score;
					if (dif == 0)
						return 0;
					else if (dif > 0)
						return -1;
					else
						return 1;
				}

			});
			
			Collections.sort(res_ww, new Comparator<Pair>() {

				@Override
				public int compare(Pair o1, Pair o2) {
					double dif = o1.score - o2.score;
					if (dif == 0)
						return 0;
					else if (dif > 0)
						return -1;
					else
						return 1;
				}

			});
			fw_wc.write(i + " ");
			fw_vec.write(i+" ");
			fw_ww.write(i + " ");
			int cur = 0;
			for (Pair entt : res_ww) {
				if (cur++ >= 10)
					break;
				double score = neg*Math.exp(entt.score)/size;
				if(score<0.0007)
					break;
				fw_ww.write(entt.index +" "); // +":"+p.score
			}
			
			cur=0;
			for (Pair entt : res_wc) {
				if (cur++ >= 10)
					break;
				double score = neg*Math.exp(entt.score)/size;
				if(score<0.001)
					break;
				fw_vec.write(entt.index +":"+neg*Math.exp(entt.score)/size+" ");
				fw_wc.write(entt.index +" "); // +":"+p.score
			}
			fw_vec.write("\r\n");
			fw_ww.write("\r\n");fw_wc.write("\r\n");
		}
		fw_vec.flush();
		fw_vec.close();
		fw_wc.flush();
		fw_wc.close();
		fw_ww.flush();
		fw_ww.close();
	}

	public static void main(String[] args) throws NumberFormatException,
			IOException {
		PPREmbedding ranker = new PPREmbedding();
		ranker.readFromFile("data/arxiv_adj_train.txt");
		ranker.generateSimilairyMap();
		// ranker.generateSimilairyMatrix();
		System.out.println("generating top k file");
		ranker.generateTopk("res/arxiv_trainout_ppr_embedding", 10);
	}
}
