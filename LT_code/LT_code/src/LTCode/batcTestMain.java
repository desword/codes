package LTCode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import share.LTPackage;

public class batcTestMain {

	
	
	 private Map<Integer, Double> prrmodel; // <pktlength, prr>
	
	 private static short totalByte = 3000;
	 private static double prr = 0.8;
	 private static  short BLOCK_SIZE = 10; // potential 10 0000 packets. then the number of blocks are seperated by the block unit size.
	private static double C;
	private static double Delta;
	 
	 
	 private static double aveTO = 0;
	 private static String detailFileName;
	 private static FileOutputStream detailFOS;
	 
	 
	 private static List<Integer> pktLossTrace;
	 private static int pktlossIndex = 0;
	 
	 
	 public static void readPrrModel(){		 
		 
		 
	 }
public static void readPacektLossTrace (String fileName) throws IOException {
		 
		 fileName = "D:\\AndroidW\\LT_code\\src\\LTCode\\lora_trace\\CODE_4bit_sf7_len200_cr4-6_bw125_cover55-255.txt";
//		 FileInputStream inputStream  =new FileInputStream(new File(fileName));
		 pktLossTrace = new ArrayList<Integer>();
		 
		 FileReader reader = new FileReader(fileName);
         BufferedReader br = new BufferedReader(reader);        
         String str = null;
        
         while((str = br.readLine()) != null) {
        	 
        	 pktLossTrace.add(Integer.parseInt(str));
        	 
         }
		 
		 
		 
	 }
	 
	 
	 
	 public static void runOnce() throws IOException {
		 
		 
//		    DaReEncoder encoder = new DaReEncoder("D:\\AndroidW\\DaRe\\src\\DaReCode\\send.txt");
		 
		 
		 LTEncoder encoder = new LTEncoder();
			FileOutputStream outputStream  =new FileOutputStream(new File("D:\\AndroidW\\LT_code\\src\\LTCode\\channel.txt"));
			
//			encoder.setR(R);
//			encoder.setW(W);
//			encoder.setDegree(degree);
			encoder.setBlockSize(BLOCK_SIZE);      
			encoder.initial("D:\\AndroidW\\LT_code\\src\\LTCode\\send.txt", C, Delta);
	       
	       
	       int index = 0;
	       while (index < 1000) {
	           byte[] pack;
	           pack = encoder.getNextPackage();
//	           if(pack == null)// if all of the src blocks have been consumed.
//	           {
//	           		break;
//	           }
	           
//	           double tmp = Math.random();
//	           if (tmp > prr){// if the packets are corrupted.
//	        	   for(int i=0 ;i< 2 ;i++)// corrupted the packet to lead to the packet loss.
//	               {
//	            	   pack[LTPackage.CRC_DATA_OFFSET + i] = 0;
//	               }
//	           }
	           
	           if(pktlossIndex >= pktLossTrace.size() ){
					 pktlossIndex = 0;
				 }
				 if(pktLossTrace.get(pktlossIndex++)==0){
					 for(int i=0 ;i< 2 ;i++)// corrupted the packet to lead to the packet loss.
	               {
	            	   pack[LTPackage.CRC_DATA_OFFSET + i] = 0;
	               }					 
				 }
	           
	           
	           outputStream.write(pack);
	           index++;
	       }
	       outputStream.close();
//	       System.out.print("\n --tx over\n");
	       
	       
	       
	       LTDecoder decoder = new LTDecoder("D:\\AndroidW\\LT_code\\src\\LTCode\\copy.txt");	
		   	FileInputStream inputStream  =new FileInputStream(new File("D:\\AndroidW\\LT_code\\src\\LTCode\\channel.txt"));
		 // [@FUNC] at this position add the C and delta.
		   	decoder.setC(C);
		   	decoder.setDelta(Delta);		   	
		   	decoder.decode(inputStream);
		   	
		   	
		   	
		   	int recBlocks = totalByte/BLOCK_SIZE;
		   	int txBlocks = (int) decoder.getNeededBlocks();
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
		
		/*
		 * 
		// The LT code testing is different from the DaRe code. 
		//We mainly test the LT performance in terms of differnet number of blocks. 
		 * considering that maximum packet size of 255 bytes, then the maximum number of blocks should be nearly 25 with 10 bytes each.
		 * the outer big packets are represented as the repetation times now and will be 10000 pkts.
		*/
		
		

//		double[] prrSet = {0.9, 0.8, 0.7, 0.6, 0.5};
//		short[] BLOCK_SIZESet = {120, 150, 200, 375, 750, 1500};
		short[] BLOCK_SIZESet = {120, 150, 200, 375, 750, 1500};// the number of blocks are the factor of 20 is ok. consider the total size of 3000 bytes.
		double[] prrSet = {0.9};
//		short[] BLOCK_SIZESet = {120};
		
		double[] CSet = {0.0001, 0.0005 ,0.001, 0.005, 0.01, 0.05, 0.1, 0.5 };
		double[] DeltaSet = {0.01, 0.1, 0.2, 0.5,0.7, 0.9};
		
//		double[] prrSet = {0.9};
//		short[] BLOCK_SIZESet = {120, };// the number of blocks are the factor of 20 is ok. consider the total size of 3000 bytes.
//		double[] CSet = {0.0001, 0.0005  };
//		double[] DeltaSet = {0.01, 0.1};
		
		 
		 
		int repTime = 500;
		
		readPacektLossTrace("test");
		
		for(short ibs:BLOCK_SIZESet)
		{
			for(double iprr:prrSet)
			{
				
//				if(ibs == 120 && iprr == 0.9){
//					continue;
//				}
					
					
				double bestC = 0;
				double bestDelta = 0;
				double minTo = 9999;
				
				detailFileName = "D:\\AndroidW\\LT_Code\\src\\LTCode\\log\\";
				detailFileName += ("bs" + ibs + "_bc" + (totalByte/ibs) + "_prr" + iprr);
				detailFileName += (".txt");
				detailFOS = new FileOutputStream(new File(detailFileName));
							
				
				for(double iC:CSet)
				{
					for(double iD:DeltaSet)
					{
						
						StringBuffer sb = new StringBuffer();
						sb.append("======C:" +  iC + "\tDelta:" + iD + "\n");
						detailFOS.write(sb.toString().getBytes());
						
						BLOCK_SIZE = ibs;
						prr = iprr;
						C = iC;
						Delta = iD;
						
						for(int i=0 ;i< repTime; i++)
						{
							runOnce();
						}
						
						aveTO = aveTO/repTime;
						if(aveTO < minTo){
							minTo = aveTO;
							bestC = iC;
							bestDelta = iD;
						}						
						
						
						
					}
				}
				System.out.printf("The best param for PRR:%f, bs:%d, bc:%d, TO:%f, C:%f, Delta:%f \n", iprr, ibs ,(totalByte/ibs),  minTo, bestC, bestDelta);
				StringBuffer sb = new StringBuffer();
				sb.append("The best param for PRR:" + iprr + "\tBC:" +(totalByte/ibs)  +  "\tTO:" + minTo + "\tC:" +  bestC + "\tDelta:" + bestDelta + "\n");
				detailFOS.write(sb.toString().getBytes());
				
				detailFOS.close();
				
			}
		}
		

		 

		
	   	

	}

}
