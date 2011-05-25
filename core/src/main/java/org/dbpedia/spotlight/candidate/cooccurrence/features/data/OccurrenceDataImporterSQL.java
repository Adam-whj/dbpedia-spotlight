package org.dbpedia.spotlight.candidate.cooccurrence.features.data;

import au.com.bytecode.opencsv.CSVReader;
import org.dbpedia.spotlight.candidate.cooccurrence.CandidateUtil;
import org.dbpedia.spotlight.exceptions.ConfigurationException;
import org.dbpedia.spotlight.model.SpotlightConfiguration;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;

/**
 * Occurrence data importer.
 *
 * Reads CSV files with unigram, bigram and trigram data and writes them into
 * the JDBC database defined in server.properties.
 *
 * @author Joachim Daiber
 */
public class OccurrenceDataImporterSQL {

	Connection sqlConnection;
	SpotlightConfiguration spotlightConfiguration;

	public OccurrenceDataImporterSQL() throws ConfigurationException {

		try {
			spotlightConfiguration = new SpotlightConfiguration("conf/server.properties");
		} catch (ConfigurationException e) {
			e.printStackTrace();
		}

		try {
			Class.forName(spotlightConfiguration.getCandidateDatabaseDriver()).newInstance();

			this.sqlConnection = DriverManager.getConnection(spotlightConfiguration.getCandidateDatabaseConnector(),
					spotlightConfiguration.getCandidateDatabaseUser(),
					spotlightConfiguration.getCandidateDatabasePassword()
					);

		} catch (SQLException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}

	}


	public void createTables() throws SQLException {

		Statement statement = this.sqlConnection.createStatement();
		statement.execute(
				"set scriptformat BINARY;" +
				"create memory table words ( id Int, word Varchar(30) primary key,  count_corpus BigInt,  count_web BigInt, UNIQUE (id) );\n" +
				"create memory table bigrams (word1 Int, word2 Int, count_corpus BigInt, significance_corpus Float, count_web BigInt, significance_web Float, Primary key (word1, word2));\n" +
				"create memory table trigrams (word1 Int, word2 Int, word3 Int, count_web BigInt, Primary key (word1, word2, word3));");

	}

	public void importCSV(File unigrams, File bigrams, File trigrams) throws IOException, SQLException {

		CSVReader csvReaderUnigram = new CSVReader(new FileReader(unigrams));

		String[] line;
		PreparedStatement unigramInsert
				= this.sqlConnection.prepareStatement("INSERT INTO words VALUES (?, ?, ?, ?);");
		while((line = csvReaderUnigram.readNext()) != null) {
			if(line.length != 4)
				break;

			unigramInsert.setInt(1, Integer.parseInt(line[0]));
			unigramInsert.setString(2, line[1]);
			unigramInsert.setLong(3, line[2].length() == 0 ? 0 : Long.parseLong(line[2]));
			unigramInsert.setLong(4, line[3].length() == 0 ? 0 : Long.parseLong(line[3]));

			unigramInsert.addBatch();
		}
		unigramInsert.executeBatch();

		CSVReader csvReaderBigrams = new CSVReader(new FileReader(bigrams));
		PreparedStatement bigramInsert
				= this.sqlConnection.prepareStatement("INSERT INTO bigrams VALUES (?, ?, ?, ?, ?, ?);");
		while((line = csvReaderBigrams.readNext()) != null) {
			if(line.length != 6)
				break;
			float significance_web = Float.parseFloat(line[5]);

			if(significance_web <
					Math.max(CandidateUtil.BIGRAM_LEFT_SIGNIFICANCE_TRESH_WEB,
							 CandidateUtil.BIGRAM_RIGHT_SIGNIFICANCE_TRESH_WEB))
				continue;

			bigramInsert.setInt(1, Integer.parseInt(line[0]));
			bigramInsert.setInt(2, Integer.parseInt(line[1]));
			bigramInsert.setInt(3, Integer.parseInt(line[2]));
			bigramInsert.setFloat(4, Float.parseFloat(line[3]));
			bigramInsert.setLong(5, Long.parseLong(line[4]));
			bigramInsert.setFloat(6, significance_web);

			bigramInsert.addBatch();
		}
		bigramInsert.executeBatch();

		CSVReader csvReaderTrigrams = new CSVReader(new FileReader(trigrams));
		PreparedStatement trigramInsert
				= this.sqlConnection.prepareStatement("INSERT INTO trigrams VALUES (?, ?, ?, ?);");

		while((line = csvReaderTrigrams.readNext()) != null) {
			if(line.length != 4)
				break;
			long count_web = Long.parseLong(line[3]);

			if(count_web <
						Math.max(CandidateUtil.TRIGRAM_LEFT_TRESH_COUNT_WEB,
								 CandidateUtil.TRIGRAM_RIGHT_TRESH_COUNT_WEB)) {
				continue;
			}


			trigramInsert.setInt(1, Integer.parseInt(line[0]));
			trigramInsert.setInt(2, Integer.parseInt(line[1]));
			trigramInsert.setInt(3, Integer.parseInt(line[2]));
			trigramInsert.setLong(4, count_web);

			trigramInsert.addBatch();
		}
		trigramInsert.executeBatch();


		sqlConnection.close();

	}

	public static void main(String[] args) throws ConfigurationException, IOException, SQLException {

		OccurrenceDataImporterSQL occurrenceDataImporterSQL = new OccurrenceDataImporterSQL();
		occurrenceDataImporterSQL.createTables();
		occurrenceDataImporterSQL.importCSV(
				new File("/Applications/XAMPP/xamppfiles/var/mysql/leipzig2/words.csv"),
				new File("/Applications/XAMPP/xamppfiles/var/mysql/leipzig2/bigrams.csv"),
				new File("/Applications/XAMPP/xamppfiles/var/mysql/leipzig2/trigrams.csv")
		);

	}


}
