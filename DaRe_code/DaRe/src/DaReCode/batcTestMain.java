package DaReCode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import share.DaRePackage;

public class batcTestMain {

	
	
	 private Map<Integer, Double> prrmodel; // <pktlength, prr>
	
	 private static double prr = 0.8;
	 private  static byte[] R = {4, 5}; // Rate is denoted first is the up, second is the bottom.  first/second. 
	 private  static short W = 4;
	 private static byte[] degree = {3, 4};
	 private static  short BLOCK_SIZE = 10; // potential 10 0000 packets. then the number of blocks are seperated by the block unit size.
	private static double aveTO = 0;
	 private static String detailFileName;
	 private static FileOutputStream detailFOS;
	 
	 private static int MaxReTxTime = 8;
	 private static int totalFileSize = 100000;
	 private static List<Integer> pktLossTrace;
	 private static int pktlossIndex = 0;
	 
	 public static void readPrrModel(){		 
		 
		 
	 }
	 
	 
	 public static void readPacektLossTraqce (String fileName) throws IOException {
		 
		 fileName = "D:\\AndroidW\\DaRe\\src\\DaReCode\\lora_trace\\CODE_4bit_sf7_len200_cr4-6_bw125_cover55-255.txt";
//		 FileInputStream inputStream  =new FileInputStream(new File(fileName));
		 pktLossTrace = new ArrayList<Integer>();
		 
		 FileReader reader = new FileReader(fileName);
         BufferedReader br = new BufferedReader(reader);        
         String str = null;
        
         while((str = br.readLine()) != null) {
        	 
        	 pktLossTrace.add(Integer.parseInt(str));
        	 
         }
		 
		 
		 
	 }
	 
	 public static int getSrcRetxTime(int lossPkts){
		 
		 int totalRetxTime = 0;
		 
		 for(int i=0 ; i< lossPkts ; i++)
		 {
			 int tmpRetx = 0;
			 while(true){
				 tmpRetx++;
//				 double tmp = Math.random();
//				 if(tmp <= prr+0.02){// considering that the small blocks may have larger PRR.
//					 break;					 
//				 }
				 
				 if(pktlossIndex >= pktLossTrace.size() ){
					 pktlossIndex = 0;
				 }
				 if(pktLossTrace.get(pktlossIndex++)==1){
					 break;
				 }
				 
			 }
			 totalRetxTime += tmpRetx;
		 }
		 return totalRetxTime;
		 
	 }
	 
	 
	 public static void runOnce() throws IOException {
//		    DaReEncoder encoder = new DaReEncoder("D:\\AndroidW\\DaRe\\src\\DaReCode\\send.txt");
		    DaReEncoder encoder = new DaReEncoder();
			FileOutputStream outputStream  =new FileOutputStream(new File("D:\\AndroidW\\DaRe\\src\\DaReCode\\channel.txt"));
			
			encoder.setR(R);
			encoder.setW(W);
			encoder.setDegree(degree);
			encoder.setBlockUniSize(BLOCK_SIZE);      
			encoder.initial("D:\\AndroidW\\DaRe\\src\\DaReCode\\send.txt");
	       
	       
	       int index = 0;
	       int extraTxBlock = 0;
	       while (true) {
	           byte[] pack;
	           pack = encoder.getNextPackage();
	           if(pack == null)// if all of the src blocks have been consumed.
	           {
	           		break;
	           }
	           
	           double tmp = Math.random();
	           
	           
	           // @ [FUN 3 and 1]no retx, considering the src and parity blcoks.
//	           if (tmp > prr){
//	        	   for(int i=0 ;i< 2 ;i++)// corrupted the packet to lead to the packet loss.
//	               {
//	            	   pack[DaRePackage.BLOCK_RATE_OFFSET + i] = 0;
//	               }
//	           }
	           
	           if(pktlossIndex >= pktLossTrace.size() ){
					 pktlossIndex = 0;
				 }
				 if(pktLossTrace.get(pktlossIndex++)==0){
					 for(int i=0 ;i< 2 ;i++)// corrupted the packet to lead to the packet loss.
	               {
	            	   pack[DaRePackage.BLOCK_RATE_OFFSET + i] = 0;
	               }					 
				 }
	           
	           
	           //@ [FUNC 2]retx the src and data pkts, setting the max retx times
//	           int currentTmp = 0;
//	           while(currentTmp < MaxReTxTime+1)
//	           {
//	        	   if (tmp <= prr){// if the packets are correctly received. then breakout 
//		        	   break;
//		           }
//	        	   currentTmp++;
//	           }
//	           extraTxBlock += currentTmp;
//	           if(currentTmp >= MaxReTxTime){// if exceed the max retx times, then label this packets are corrupted.
//	        	   for(int i=0 ;i< 2 ;i++)// corrupted the packet to lead to the packet loss.
//	               {
//	            	   pack[DaRePackage.BLOCK_RATE_OFFSET + i] = 0;
//	               }
//	           }
	           
	           
	           outputStream.write(pack);
	           index++;
	       }
	       outputStream.close();
//	       System.out.print("\n --tx over\n");
	       
	       
	       
		   	DaReDecoder decoder = new DaReDecoder("D:\\AndroidW\\DaRe\\src\\DaReCode\\copy.txt");	
		   	FileInputStream inputStream  =new FileInputStream(new File("D:\\AndroidW\\DaRe\\src\\DaReCode\\channel.txt"));
		   	decoder.decode(inputStream);
		   	
		  //@ [FUNC 1] no retx considering. while some src blocks are just lost.
//		   	int recBlocks = decoder.getRevolsedBlocks();
//		   	int txBlocks = (extraTxBlock+index)*R[1];
		   	
		  //@ [FUNC 2]retx the src and data pkts, setting the max retx times
//		   	int recBlocks = decoder.getRevolsedBlocks();
//		   	int txBlocks = (extraTxBlock+index)*R[1];
		   	
		 // @ [FUN 3]retx only src blocks.
		   	int recBlocks = decoder.getRevolsedBlocks();
		   	int extracRetxBlocks = getSrcRetxTime(totalFileSize/BLOCK_SIZE - recBlocks);
		   	recBlocks = totalFileSize/BLOCK_SIZE;
		   	int txBlocks = (index)*R[1] + extracRetxBlocks;
		   	
		   	
		   	System.out.printf("TxBlocks: %d,  RecBLocks:%d,  TO: %f \n", txBlocks, recBlocks, txBlocks*1.0/ recBlocks);
		   	StringBuffer sb = new StringBuffer();		   	 
		   	sb.append("TxBlocks:"+ txBlocks +"\tRecBLocks:"+ recBlocks +"\tTO:"+ (txBlocks*1.0/ recBlocks) +"\n");
		   	detailFOS.write(sb.toString().getBytes());
		   	
		   	aveTO += txBlocks*1.0/ recBlocks;
	 }
	
	 
	 
	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
//		double[] prrSet = {0.5, 0.6, 0.7, 0.8, 0.9};
//		 byte[][] RSet = {{1,2}, {1,3}, {1, 4}, {1, 5}, {2, 3}, {2, 5}, {4, 5}, {5, 6}}; // Rate is denoted first is the up, second is the bottom.  first/second. 
//		 short[] WSet = {1, 5, 10, 20, 40, 80};
//		 byte[][] degreeSet = {{1, 2},{3,4}, {4, 5}};// or calcualte the best fit degree according to DaRe.
//		 short[] BLOCK_SIZESet = {10, 20, 40, 50, 100, 200, 500, 1000};// the number of blocks are the factor of 20 is ok. consider the total size of 10,0000 bytes.
		 
//			double[] prrSet = {0.9, 0.8, 0.7, 0.6, 0.5};
			double[] prrSet = {0.8};
			 byte[][] RSet = {{1,2}, {2, 3}, {4, 5}}; // Rate is denoted first is the up, second is the bottom.  first/second. 
			 short[] WSet = {1, 5, 10, 20, 40, 80};
			 byte[][] degreeSet = {{1, 2},{3,4},{4, 5}};// or calcualte the best fit degree according to DaRe.
//			 short[] BLOCK_SIZESet = {10, 20, 40, 50};// the number of blocks are the factor of 20 is ok. consider the total size of 10,0000 bytes.
//			 short[] BLOCK_SIZESet = {20};
//			 short[] BLOCK_SIZESet = {40, 50};
//			 short[] BLOCK_SIZESet = {10, 50};
			 short[] BLOCK_SIZESet = {20, 40};
//		
//			double[] prrSet = {0.5};
//			 byte[][] RSet = {{1,2}}; // Rate is denoted first is the up, second is the bottom.  first/second. 
//			 short[] WSet = {1, 5};
//			 byte[][] degreeSet = {{1, 2}};// or calcualte the best fit degree according to DaRe.
//			 short[] BLOCK_SIZESet = {10, 20};// the number of blocks are the factor of 20 is ok. consider the total size of 10,0000 bytes.
			
		 
		 
			 readPacektLossTraqce("ets");
			 
			 
		int repTime = 100;
		
		for(short ibs:BLOCK_SIZESet)
		{
			for(double iprr:prrSet)
			{
				
				byte[] bestR = {0,0};
				short bestW = 0;
				byte[] bestDegree = {0,0};
				double minTo = 9999;
				
				detailFileName = "D:\\AndroidW\\DaRe\\src\\DaReCode\\log\\";
				detailFileName += ("bs" + ibs + "_bc" + (totalFileSize/ibs) + "_prr" + iprr);
				detailFileName += (".txt");
				detailFOS = new FileOutputStream(new File(detailFileName));
				
				for(byte[] iR:RSet)
				{
					for(short iW:WSet)
					{
						for(byte[] idegree:degreeSet){
							
							StringBuffer sb = new StringBuffer();
							sb.append("========R:" + (iR[0]*1.0/iR[1]) + "\tW:" + iW + "\tdegree:" + (idegree[0]*1.0/idegree[1]) + "\n");
							detailFOS.write(sb.toString().getBytes());
							
							BLOCK_SIZE = ibs;
							prr = iprr;
							R = iR.clone();
							W = iW;
							degree = idegree.clone();
							
							for(int i=0 ;i< repTime; i++)
							{
								runOnce();
							}
							
							aveTO = aveTO/repTime;
							if(aveTO < minTo){
								minTo = aveTO;
								bestR = R.clone();
								bestW = W;
								bestDegree = degree.clone();
							}
						}
						
						// best degree from DaRe.
						BLOCK_SIZE = ibs;
						prr = iprr;
						R = iR.clone();
						W = iW;
						double Dtemp = (0.5 + 0.25) * Math.exp(W * (- 1/16)) + 0.25;
						Dtemp = 0.83;
						String stmp = String.valueOf(Dtemp);
						String[] array = new String[2];
						array = stmp.split("\\.");
				        int a = Integer.parseInt(array[0]);//获取整数部分  
				        int b = Integer.parseInt(array[1]);//获取小数部分  
				        int length = array[1].length();  
				        byte FenZi = (byte) (a * Math.pow(10, length) + b);  
				        byte FenMu = (byte) Math.pow(10, length); 
				        byte[] tmpd = {FenZi,FenMu };
						degree =tmpd.clone();
						
						for(int i=0 ;i< repTime; i++)
						{
							runOnce();
						}
						
						aveTO = aveTO/repTime;
						if(aveTO < minTo){
							minTo = aveTO;
							bestR = R.clone();
							bestW = W;
							bestDegree = degree.clone();
						}
						
					}
				}
				System.out.printf("The best param for PRR:%f, bs:%d, bc:%d, TO:%f, R:%f, W:%d, degree:%f \n", iprr, ibs ,(totalFileSize/ibs),  minTo, bestR[0]*1.0/bestR[1],bestW, bestDegree[0]*1.0/bestDegree[1]);
				StringBuffer sb = new StringBuffer();
				sb.append("The best param for PRR:" + iprr + "\tBC:" +(totalFileSize/ibs)  +  "\tTO:" + minTo + "\tR:" + (bestR[0]*1.0/bestR[1]) + "\tW:" + bestW + "\tdegree:" + (bestDegree[0]*1.0/bestDegree[1]) + "\n");
				detailFOS.write(sb.toString().getBytes());
				
				detailFOS.close();
				
			}
		}
		

		 

		
	   	

	}

}
