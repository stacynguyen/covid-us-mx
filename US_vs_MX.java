import java.io.*;
import java.util.*;
import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.text.DecimalFormat;
import java.io.File;
import java.time.*;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.text.DecimalFormat;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class US_vs_MX {

	/*
		Wrapper para leer archivos de excesos de muerte del gobierno federal y CDMX
		Cifras por entidad de unidad medica de muerte      
		Casos y muertes ordenados por fecha de contagio
		El archivo del dia coincide exactamente con las cifras generadas por gobierno CDMX
		(luego ya no porque se agregan hacia atrás casos y muertes)
		
		Las funciones de lectura son un batidillo monumental.
		Abro una nueva serie para meter algo de orden. Basado en SimulaENSANUT
		
		PD. Tiene demasiada basura. El existente se va a ArchivoDiarioBack y el limpio se queda aqui
	*/
	
	static String skip = ""; 
	static DecimalFormat df0 = new DecimalFormat("#0.00");
	static DecimalFormat df1 = new DecimalFormat("#0.00");
	static DecimalFormat df3 = new DecimalFormat("##,###,###");
	static DecimalFormat df4 = new DecimalFormat("#,##0");
	
	final static double EXCESO_NAL_2020 = 325502d;	// mis calculos
	final static double EXCESO_NAL_2021 = 339897d;	
	final static double EXCESO_CDMX_2020 = 42155d;	
	final static double EXCESO_CDMX_2021 = 51000d;	// ERROR deberia ser 50,876 too late to fix
	
	final static double POBL_CDMX  = 				92.09944d;		// 100,000s, Census 2020
	final static double POBL_NAL = 					126.014024d;		// millions
										
	final static double FACTOR_USA = 				1.32d;			// cdc may 2022
	final static double FACTOR_NYC = 				1.2833d;			
	final static double POBL_USA = 					331.449281d;		// millions, USA Census 2020
	final static double POBL_NYC  = 				88.04190d;		// 100,000s, Census 2020
	
	final static int DELAY_USA = 14;				// US nat dta come by day of announcement; this moves them back
													// Arbitrary. Value >7 but unknown
													// NOTE NYC is by day of death
	
	static String path = "C:\\java\\datos covid\\lag\\";

	public static void main(String[] args) throws Exception { 
		
		ArchivoDiario f1 = new ArchivoDiario(args[0]);
		System.out.println("Numero de registros       = "+f1.rr());
		System.out.println("Numero de errores         = "+f1.err());
		System.out.println("Numero de casos positivos = "+f1.cc());
		System.out.println("Numero de muertes covid   = "+f1.dd());
		
		String file1 = "c:/java/MX vs USA.csv";
		String file2 = "c:/java/CDMX vs NYC.csv";
		PrintStream out1 = new PrintStream(new File(file1));
		out1.println("location,date,deaths,factor,probable_avg_7");
		PrintStream out2 = new PrintStream(new File(file2));
		out2.println("location,date,deaths,factor,probable_avg_7");

		print1(out1,f1); 	// MX 
		print2(out1); 		// USA
		out1.close();
		
		print3(out2,f1); 	// CDMX
		print4(out2); 		// NYC
		out2.close();
	}
	
	public static void print1(PrintStream out, ArchivoDiario f1) throws Exception { 

		int[] md = f1.muertes_dia();		// NAL
		double mm = 0;						// muertes registradas 2020 NAL
		int liminf = dia_epi("2020-01-01");
		int limsup = dia_epi("2020-12-31"); 
		for (int i=liminf; i<=limsup; i++) mm = mm + md[i]; 
		double factor_2020_nal = EXCESO_NAL_2020 / mm ;

		mm = 0;								// muertes registradas 2021 NAL
		liminf = dia_epi("2021-01-01");
		limsup = dia_epi("2021-12-31"); 
		for (int i=liminf; i<=limsup; i++) mm = mm + md[i]; 
		double factor_2021_nal = EXCESO_NAL_2021 / mm ;

		mm = 0;								// muertes registradas 2022 NAL
		liminf = dia_epi("2022-01-01");
		limsup = dia_epi("2022-03-05"); 
		for (int i=liminf; i<=limsup; i++) mm = mm + md[i]; 
		System.out.println("Muertes nacionales 2022, estimadas a SE 9 : "+ factor_2021_nal * mm );
			
		liminf = dia_epi("2020-01-01");
		limsup = dia_epi("2022-05-07");
		md = f1.muertes_dia();
		mm = 0;  double mm_nal = 0;
		System.out.println("Muertes nacionales, por dia de fallecimiento");
		for (int i=liminf; i<=limsup; i++) {
			String fecha = fecha_dia_epi(i);
			double factor = 0;
			if (fecha.startsWith("2020")) factor = factor_2020_nal;
			if (fecha.startsWith("2021")) factor = factor_2021_nal;
			if (fecha.startsWith("2022")) factor = factor_2021_nal;	// until we know more
			
			
			mm = mm + md[i];
			mm_nal = mm_nal + md[i] * factor;
			double promedio = promedio(i,7,md) * factor / POBL_NAL;// 7 day-smoothed ESTIMATED avg per million

			out.print("MX"		+","+skip);
			out.print(	fecha				+","+skip);
			out.print(	md[i] 				+","+skip);
			out.print(	factor 				+","+skip);
			// out.print(	promedio(i,7,md) 	+","+skip);
			out.print(	promedio			);
			// out.print(	mm					);
			out.println(	 );
		}
		System.out.println("Total muertes nacional reportado al "+fecha_dia_epi(limsup)+" :" + mm);
		System.	out.println("Total muertes nacional estimado al "+fecha_dia_epi(limsup)+" :" + mm_nal);
		System.	out.println("Total muertes nacional por millon estimado al "+fecha_dia_epi(limsup)+" :" + mm_nal/POBL_NAL);
		
	}

	public static void print3(PrintStream out, ArchivoDiario f1) throws Exception { 

		int[] md = f1.muertes_estado_dia(9);		// CDMX
		double mm = 0;								// muertes registradas 2020 CDMX
		int liminf = dia_epi("2020-01-01");
		int limsup = dia_epi("2020-12-31"); 
		for (int i=liminf; i<=limsup; i++) mm = mm + md[i]; 
		double factor_2020_cdmx = EXCESO_CDMX_2020 / mm ;

		mm = 0;								// muertes registradas 2021 CDMX
		liminf = dia_epi("2021-01-01");
		limsup = dia_epi("2021-12-31"); 
		for (int i=liminf; i<=limsup; i++) mm = mm + md[i]; 
		double factor_2021_cdmx = EXCESO_CDMX_2021 / mm ;
		
		mm = 0;								
		liminf = dia_epi("2022-01-01");
		limsup = dia_epi("2022-03-05"); 
		for (int i=liminf; i<=limsup; i++) mm = mm + md[i]; 
		System.out.println("Muertes CDMX  2022, estimadas a SE 9 : "+ factor_2021_cdmx * mm );
		
		mm = 0;								
		liminf = dia_epi("2022-01-01");
		limsup = dia_epi("2022-05-07"); 
		for (int i=liminf; i<=limsup; i++) mm = mm + md[i]; 
		System.out.println("Muertes CDMX  2022, estimadas a 5 mayo 2022 : "+ factor_2021_cdmx * mm );		

		liminf = dia_epi("2020-01-01");
		limsup = dia_epi("2022-05-07"); 
		md = f1.muertes_estado_dia(9); 
		mm = 0; double mm_nal = 0;
		System.out.println("Muertes CDMX, por dia de fallecimiento");
		for (int i=liminf; i<=limsup; i++) {
			String fecha = fecha_dia_epi(i);
			double factor = 0;
			if (fecha.startsWith("2020")) factor = factor_2020_cdmx;
			if (fecha.startsWith("2021")) factor = factor_2021_cdmx;
			if (fecha.startsWith("2022")) factor = factor_2021_cdmx;	// until we know more
			
			mm = mm + md[i];
			mm_nal = mm_nal + md[i] * factor;
			double promedio = promedio(i,7,md) * factor / POBL_CDMX;// 7 day-smoothed ESTIMATED avg per million
			out.print("CDMX"		+","+skip);
			out.print(	fecha				+","+skip);
			out.print(	md[i] 				+","+skip);
			out.print(	factor 				+","+skip);
			out.print(	promedio			);
			// out.print(	mm					);
			out.println(	 );
		}
		System.out.println("Total muertes CDMX reportado al "+fecha_dia_epi(limsup)+" :" + mm);
		System.out.println("Total muertes CDMX estimado al "+fecha_dia_epi(limsup)+" :" + mm_nal);
		System.out.println("Total muertes CDMX por 100 mil estimado al "+fecha_dia_epi(limsup)+" :" + mm_nal/POBL_CDMX);
			
	}
	
	// Muertes en dia i, promedio i + (w-1) dias anteriores
	static float promedio(int i, int w, int[] m) {
		float max = 0; float n = 0;
		for (int k=0; k<w; k++) if ((i-k)>=0) { max = max+m[i-k]; n++;  }
		return max/n;
	}	

	// owid
	static String[] header1 = { "iso_code", "continent", "location", "date", "total_cases", "new_cases", "new_cases_smoothed",
"total_deaths", "new_deaths", "new_deaths_smoothed", "total_cases_per_million", "new_cases_per_million",
"new_cases_smoothed_per_million", "total_deaths_per_million", "new_deaths_per_million", "new_deaths_smoothed_per_million",
"reproduction_rate", "icu_patients", "icu_patients_per_million", "hosp_patients", "hosp_patients_per_million",
"weekly_icu_admissions", "weekly_icu_admissions_per_million", "weekly_hosp_admissions", 
"weekly_hosp_admissions_per_million", "total_tests", "new_tests", "total_tests_per_thousand", "new_tests_per_thousand",
"new_tests_smoothed", "new_tests_smoothed_per_thousand", "positive_rate", "tests_per_case", "tests_units",
"total_vaccinations", "people_vaccinated", "people_fully_vaccinated", "total_boosters", "new_vaccinations",
"new_vaccinations_smoothed", "total_vaccinations_per_hundred", "people_vaccinated_per_hundred", 
"people_fully_vaccinated_per_hundred", "total_boosters_per_hundred", "new_vaccinations_smoothed_per_million",
"new_people_vaccinated_smoothed", "new_people_vaccinated_smoothed_per_hundred", "stringency_index", "population",
"population_density", "median_age", "aged_65_older", "aged_70_older", "gdp_per_capita", "extreme_poverty", 
"cardiovasc_death_rate", "diabetes_prevalence", "female_smokers", "male_smokers", "handwashing_facilities",
"hospital_beds_per_thousand", "life_expectancy", "human_development_index", "excess_mortality_cumulative_absolute",
"excess_mortality_cumulative", "excess_mortality", "excess_mortality_cumulative_per_million" };
	
	static int field_is(String id, String[] h) { for (int i=0; i<h.length; i++) if (h[i].equals(id)) return i; return -1; }
	
	public static void print2(PrintStream out) throws Exception { 
	
		int ilo = 	field_is("location", header1);	
		int ida = 	field_is("date", header1);	
		int ide = 	field_is("new_deaths", header1);
		
		String file = "C:/Users/arrio/Downloads/owid-covid-data 17 may 2022.csv";
		String data0 = null;
		String data1 = null;
		String[] data2 = null;
		int[] md = new int[366+365+365]; int pp=0;
		
		BufferedReader in1 = new BufferedReader (new InputStreamReader (new FileInputStream(file),"UTF-8"));
		data0=in1.readLine();								// Header, skip after calculating no.cols
		int err = 0;
		double mmd = 0;
		while ( (data0=in1.readLine())!=null ) {

			try {
				data2 = data0.split(",");
				String loc = data2[0].trim();
				if (!loc.equals("USA")) continue;
				String date = data2[ida];
				String actual_date = fecha_dia_epi(dia_epi(date)-DELAY_USA);
				Double m = 0d;
				try { m = Double.parseDouble(data2[ide]); } catch (Exception ee){}//System.out.println(ee);}
				md[pp] = m.intValue();
				mmd = mmd + md[pp] * FACTOR_USA;
				double promedio = promedio(pp,7,md) * FACTOR_USA / POBL_USA;// 7 day-smoothed ESTIMATED avg per million

				out.print(loc		+","+skip);
				out.print(actual_date		+","+skip);
				out.print(md[pp]			+","+skip);
				out.print(FACTOR_USA		+","+skip);
				out.print(promedio);
				out.println();

				pp++;
								
			} catch (Exception e0) { 
				System.out.println(e0);
				err++; 
			}		
		}
		in1.close();
		System.	out.println("Total muertes EEUU por millon estimado  :" + mmd/POBL_USA);

	} 
	
	static String[] header2 = {
"date_of_interest", "CASE_COUNT", "PROBABLE_CASE_COUNT", "HOSPITALIZED_COUNT", "DEATH_COUNT", "PROBABLE_DEATH_COUNT", 
"CASE_COUNT_7DAY_AVG", "ALL_CASE_COUNT_7DAY_AVG", "HOSP_COUNT_7DAY_AVG", "DEATH_COUNT_7DAY_AVG", "ALL_DEATH_COUNT_7DAY_AVG", 
"BX_CASE_COUNT", "BX_PROBABLE_CASE_COUNT", "BX_HOSPITALIZED_COUNT", "BX_DEATH_COUNT", "BX_PROBABLE_DEATH_COUNT", 
"BX_CASE_COUNT_7DAY_AVG", "BX_PROBABLE_CASE_COUNT_7DAY_AVG", "BX_ALL_CASE_COUNT_7DAY_AVG", "BX_HOSPITALIZED_COUNT_7DAY_AVG", 
"BX_DEATH_COUNT_7DAY_AVG", "BX_ALL_DEATH_COUNT_7DAY_AVG", "BK_CASE_COUNT", "BK_PROBABLE_CASE_COUNT", "BK_HOSPITALIZED_COUNT", 
"BK_DEATH_COUNT", "BK_PROBABLE_DEATH_COUNT", "BK_CASE_COUNT_7DAY_AVG", "BK_PROBABLE_CASE_COUNT_7DAY_AVG", 
"BK_ALL_CASE_COUNT_7DAY_AVG", "BK_HOSPITALIZED_COUNT_7DAY_AVG", "BK_DEATH_COUNT_7DAY_AVG", "BK_ALL_DEATH_COUNT_7DAY_AVG", 
"MN_CASE_COUNT", "MN_PROBABLE_CASE_COUNT", "MN_HOSPITALIZED_COUNT", "MN_DEATH_COUNT", "MN_PROBABLE_DEATH_COUNT", 
"MN_CASE_COUNT_7DAY_AVG", "MN_PROBABLE_CASE_COUNT_7DAY_AVG", "MN_ALL_CASE_COUNT_7DAY_AVG", "MN_HOSPITALIZED_COUNT_7DAY_AVG",
 "MN_DEATH_COUNT_7DAY_AVG", "MN_ALL_DEATH_COUNT_7DAY_AVG", "QN_CASE_COUNT", "QN_PROBABLE_CASE_COUNT", "QN_HOSPITALIZED_COUNT",
 "QN_DEATH_COUNT", "QN_PROBABLE_DEATH_COUNT", "QN_CASE_COUNT_7DAY_AVG", "QN_PROBABLE_CASE_COUNT_7DAY_AVG", 
"QN_ALL_CASE_COUNT_7DAY_AVG", "QN_HOSPITALIZED_COUNT_7DAY_AVG", "QN_DEATH_COUNT_7DAY_AVG", "QN_ALL_DEATH_COUNT_7DAY_AVG",
 "SI_CASE_COUNT", "SI_PROBABLE_CASE_COUNT", "SI_HOSPITALIZED_COUNT", "SI_DEATH_COUNT", "SI_PROBABLE_DEATH_COUNT",
 "SI_CASE_COUNT_7DAY_AVG", "SI_PROBABLE_CASE_COUNT_7DAY_AVG", "SI_ALL_CASE_COUNT_7DAY_AVG", "SI_HOSPITALIZED_COUNT_7DAY_AVG",
 "SI_DEATH_COUNT_7DAY_AVG", "SI_ALL_DEATH_COUNT_7DAY_AVG", "INCOMPLETE"		
	};
	
	public static void print4(PrintStream out) throws Exception { 
	
		int ida = 	field_is("date_of_interest", header2);	
		int ide = 	field_is("DEATH_COUNT", header2);
		int idep = 	field_is("PROBABLE_DEATH_COUNT", header2);
		
		String file = "C:/Users/arrio/Downloads/NYC covid data-by-day.csv";
		String data0 = null;
		String data1 = null;
		String[] data2 = null;
		int[] md = new int[366+365+365]; int pp=0;
		
		BufferedReader in1 = new BufferedReader (new InputStreamReader (new FileInputStream(file),"UTF-8"));
		data0=in1.readLine();								// Header, skip after calculating no.cols
		float dif1 = 0;
		int err = 0;
		double mmt = 0;
		while ( (data0=in1.readLine())!=null ) {

			try {
				data2 = data0.split(",");
				String date = data2[ida];
				String[] d1 = date.split("/"); date = d1[2] + "-" + d1[0] + "-" + d1[1];
				Double m = 0d;
				try { 
					m = Double.parseDouble(data2[ide]) ; // + Double.parseDouble(data2[idep]); 
				} catch (Exception ee){}//System.out.println(ee);}
				md[pp] = m.intValue();
				mmt = mmt + md[pp]*FACTOR_NYC;
				double promedio = promedio(pp,7,md) * FACTOR_NYC / POBL_NYC;// 7 day-smoothed ESTIMATED avg per million

				out.print("NYC"		+","+skip);
				out.print(date		+","+skip);
				out.print(md[pp]			+","+skip);
				out.print(FACTOR_USA		+","+skip);
				out.print(promedio);
				out.println();

				pp++;
								
			} catch (Exception e0) { 
				System.out.println(e0);
				err++; 
			}		
		}
		in1.close();
		System.out.println("Muertes totales NYC por 100 mil estimado "+mmt/POBL_NYC);
	} 
	
	// calcula dia epidemiologico, a partir de 0 = 1 ene 2020
	static int dia_epi(String fecha) throws Exception { return FechasNuevaSerie.dia_epi(fecha); }

	// calcula semana epidemiologica REAL, a partir de 1
	static int semana_epi(String fecha) throws Exception { return FechasNuevaSerie.semana_epi(fecha); }	

	// Mod para retroceder 5 dias antes de fecha de sintomas para estimar contagio
	static int semana_epi_menos_n(String fecha, int n) throws Exception { return FechasNuevaSerie.semana_epi_menos_n(fecha,n); }	
	
	static String fecha_dia_epi(int fecha) throws Exception { return FechasNuevaSerie.fecha_dia_epi(fecha); }


}
