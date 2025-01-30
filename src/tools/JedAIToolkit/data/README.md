This folder contains several datasets that have been tested with JedAI. We have grouped them in two categories: 
1) those suitable for Clean-Clean ER, and 
2) those sutable for Dirty ER. 

Every dataset includes two types of files: 
1) those containing the entity profiles themselves, which are called **entity files**, and 
2) those containing the golden standard (i.e., the ground-truth with the real matches), which are called **groundtruth files**.

Note that every file that is available as a *Java Serialized Object* (**JSO**) can be read using the class DataReader.EntityReader.EntitySerializationReader for entity files or the class DataReader.GroundTruthReader.GtSerializationReader for ground-truth files. See [this class](https://github.com/scify/JedAIToolkit/blob/master/jedai-core/tests/GeneralExamples/DirtyErDatasetStatistics.java) for an example.

# Clean-Clean ER datasets

| Dataset Name | D1 Entities | D2 Entities | D1 Name-Value Pairs | D2 Name-Value Pairs | Duplicates | Average NVP per Entity | Brute-force Comparisons | File Format | Data Origin | 
| --- | --- | --- | --- | --- | --- |--- | --- | --- | --- | 
| Restaurants | 339 | 2,256 | 1,130 | 7,519 | 89 | 3.3 | 7.64E+05 | JSO ([Rest. 1 file](cleanCleanErDatasets/restaurant1Profiles), [Rest. 2 file](cleanCleanErDatasets/restaurant2Profiles), [groundtruth file](cleanCleanErDatasets/restaurantsIdDuplicates)) | Real data |
| Abt-Buy | 1,076 | 1,076 | 2,568 | 2,308 | 1,076 | 2.4 | 1.16E+06 | JSO ([Abt entity file](cleanCleanErDatasets/abtProfiles), [Buy entity file](cleanCleanErDatasets/buyProfiles), [groundtruth file](cleanCleanErDatasets/abtBuyIdDuplicates)) | Real data |
| Amazon-Google Products | 1,354 | 3,039 | 5,302 | 9,110 | 1,104 | 3.9 | 4.11E+06 | JSO ([Amazon entity file](cleanCleanErDatasets/amazonProfiles), [GP entity file](cleanCleanErDatasets/gpProfiles), [groundtruth file](cleanCleanErDatasets/amazonGpIdDuplicates)) | Real data |
| DBLP-ACM | 2,616 | 2,294 | 10,464	| 9,162 | 2,224 | 4.0 | 6.00E+06 | JSO ([DBLP entity file](cleanCleanErDatasets/dblpProfiles), [ACM entity file](cleanCleanErDatasets/acmProfiles), [groundtruth file](cleanCleanErDatasets/dblpAcmIdDuplicates)), CSV ([DBLP entity file](cleanCleanErDatasets/DBLP-ACM/DBLP2.csv), [ACM entity file](cleanCleanErDatasets/DBLP-ACM/ACM.csv)), XML ([DBLP entity file](cleanCleanErDatasets/DBLP2toRdf.xml), [ACM entity file](cleanCleanErDatasets/ACMtoRdf.xml)) | Real data |
| IMDB-TMDB | 5,118 | 6,056 | 21,294 | 23,761 | 1,968 | 4.0 | 3.10E+07 | JSO ([IMDB entity file](cleanCleanErDatasets/imdbProfilesNEW), [TMDB entity file](cleanCleanErDatasets/tmdbProfiles), [groundtruth file](cleanCleanErDatasets/imdbTmdbIdDuplicates))| Real data |
| IMDB-TVDB | 5,118 | 7,810 | 21,294 | 20,902 | 1,072 | 3.2 | 4.00E+07 | JSO ([IMDB entity file](cleanCleanErDatasets/imdbProfilesNEW), [TVDB entity file](cleanCleanErDatasets/tvdbProfiles), [groundtruth file](cleanCleanErDatasets/imdbTvdbIdDuplicates))| Real data |
| TMDB-TVDB | 6,056 | 7,810 | 23,761 | 20,902 | 1,095 | 2.2 | 4.73E+07 | JSO ([TMDB entity file](cleanCleanErDatasets/tmdbProfiles), [TVDB entity file](cleanCleanErDatasets/tvdbProfiles), [groundtruth file](cleanCleanErDatasets/tmdbTvdbIdDuplicates))| Real data |
| Amazon-Walmart | 2,554 | 22,074 | 14,143 | 114,315 | 853 | 5.2 | 5.64E+07 | JSO ([Amazon entity file](cleanCleanErDatasets/amazonProfiles2), [Walmart entity file](cleanCleanErDatasets/walmartProfiles), [groundtruth file](cleanCleanErDatasets/amazonWalmartIdDuplicates)) | Real data |
| DBLP-Scholar | 2,516 | 61,353 | 10,064 | 198,001 | 2,308 | 4.0 | 1.54E+08 | JSO ([DBLP entity file](cleanCleanErDatasets/dblpProfiles2), [Scholar entity file](cleanCleanErDatasets/scholarProfiles), [groundtruth file](cleanCleanErDatasets/dblpScholarIdDuplicates))| Real data |
| Movies | 27,615 | 23,182 | 155,436 | 816,009 | 22,863 | 5.6 | 6.40E+08|JSO ([IMDB entity file](cleanCleanErDatasets/imdbProfiles), [DBPedia entity file](cleanCleanErDatasets/dbpediaProfiles.zip), [groundtruth file](cleanCleanErDatasets/moviesIdDuplicates)) | Real data |
| DBPedia | 1,190,733 | 2,164,040 | 1.69E+07 | 3.50E+07 | 892,586 | 14.2 | 2.58E+12 | [JSO](https://data.mendeley.com/datasets/4whpm32y47/7) | Real data |


# Dirty ER datasets

| Dataset Name | Entities | Name-Value Pairs | Duplicates | Average NVP per Entity | Brute-force Comparisons | File Format | Data Origin | 
| --- | --- | --- | --- | --- | --- | --- | --- |
| Restaurant | 864 | 4,319 | 112 | 5.0 | 3.73E+05 | JSO ([entity file](dirtyErDatasets/restaurantProfiles), [groundtruth file](dirtyErDatasets/restaurantIdDuplicates)) | Real data |
| Census | 841 | 3,913 | 344 | 4.7 | 3.53E+05 | JSO ([entity file](dirtyErDatasets/censusProfiles), [groundtruth file](dirtyERfiles/censusIdDuplicates)) | Real data |
| Cora | 1,295 | 7,166 | 17,184 | 5.5 | 8.38E+05 | JSO ([entity file](dirtyErDatasets/coraProfiles), [groundtruth file](dirtyErDatasets/coraIdDuplicates)) | Real data |
| CdDb | 9763 | 173,309	| 299 |	17.8 | 4.77E+07 | JSO ([entity file](dirtyErDatasets/cddbProfiles), [groundtruth file](dirtyErDatasets/cddbIdDuplicates)) | Real data |
| Abt-By | 2,152 | 4,876 | 1,076 | 2.3 | 2.31E+06 | JSO ([entity file](dirtyErDatasets/abtBuyProfiles), [groundtruth file](dirtyErDatasets/abtBuyIdDuplicates)) | Real data |
| DBLP-ACM | 4,910 | 19,626	| 2,224	| 4.0 | 1.21E+07 | JSO ([entity file](dirtyErDatasets/dblpAcmProfiles), [groundtruth file](dirtyErDatasets/dblpAcmIdDuplicates)) | Real data |
| DBLP-Scholar | 63,869 | 208,065 | 2,308 | 3.3	| 2.04E+09 | JSO ([entity file](dirtyErDatasets/dblpScholarProfiles), [groundtruth file](dirtyErDatasets/dblpScholarIdDuplicates)) | Real data |
| Amazon-GP	| 4,393	| 14,412 | 1,104 | 3.3 | 9.65E+06 | JSO ([entity file](dirtyErDatasets/amazonGpProfiles), [groundtruth file](dirtyErDatasets/amazonGpIdDuplicates)) | Real data |
| Movies | 50,797 |	971,445	| 22,863 | 19.1 | 1.29E+09 | JSO ([zipped entity file](dirtyErDatasets/moviesProfiles.rar), [groundtruth file](dirtyErDatasets/moviesIdDuplicates)) | Real data |
<!---| DBPedia	| 3,354,773	| 5.19E+07| 	892,586	| 15.5	| 5.63E+12| JSO | Real data |-->





