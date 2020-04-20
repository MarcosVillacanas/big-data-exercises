package nearsoft.academy.bigdata.recommendation;

import java.io.*;
import java.util.*;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.common.FastByIDMap;
import org.apache.mahout.cf.taste.impl.model.GenericPreference;
import org.apache.mahout.cf.taste.impl.neighborhood.NearestNUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.CachingRecommender;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.model.PreferenceArray;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;

public class MovieRecommender {
    private final String path;
    private ArrayList<ArrayList<GenericPreference>> data;
    private HashMap<String, Integer> userTraductor;
    private BiMap<String, Integer> productTraductor;
    private int userIntegerId = 0;
    private int productIntegerId = 0;

    public MovieRecommender(final String path) throws IOException {
        this.path = path;
        data = new ArrayList();
        userTraductor = new HashMap();
        productTraductor = HashBiMap.create();
        BufferedReader reader = new BufferedReader(new FileReader(this.path));
        String line = "";
        String productCode = "";
        String userCode = "";
        while ((line = reader.readLine()) != null) {
            if (line.trim().length() <= 0) continue;
            String[] duo = line.split("/");
            if (duo.length != 2) continue;
            if (duo[0].equals("product")) {
                String[] separator = duo[1].split(":");
                productCode = separator[1].trim();
                if (!productTraductor.containsKey(productCode)) {
                    productTraductor.put(productCode, productIntegerId);
                    productIntegerId++;
                }
            } else {
                String[] separator = duo[1].split(":");
                if (separator[0].equals("userId")) {
                    userCode = separator[1].trim();
                    if (!userTraductor.containsKey(userCode)) {
                        userTraductor.put(userCode, userIntegerId);
                        data.add(new ArrayList());
                        userIntegerId++;
                    }
                } else if (separator[0].equals("score")) {
                    data.get(userTraductor.get(userCode)).add(new GenericPreference(userTraductor.get(userCode),
                            productTraductor.get(productCode), Float.valueOf(separator[1].trim())));
                }
            }
        }
        File newFile = new File("src\\resources\\mahoutData.csv");
        newFile.createNewFile();
        FileWriter myWriter = new FileWriter("src\\resources\\mahoutData.csv");
        for (int i = 0; i < data.size(); i++) {
            for (int j = 0; j < data.get(i).size(); j++) {
                myWriter.write(data.get(i).get(j).getUserID() + "," +
                        data.get(i).get(j).getItemID() + "," + data.get(i).get(j).getValue() + "\n");
            }
        }
        myWriter.close();
    }

    public int getTotalReviews() {
        int reviews = 0;
        for (ArrayList<GenericPreference> userReviews: data) {
            reviews += userReviews.size();
        }
        return reviews;
    }

    public int getTotalProducts() {
        return productTraductor.size();
    }

    public int getTotalUsers() {
        return userTraductor.size();
    }

    public List<String> getRecommendationsForUser(final String userCode) throws IOException, TasteException {
        DataModel model = new FileDataModel(new File("src\\resources\\mahoutData.csv"));
        UserSimilarity userSimilarity = new PearsonCorrelationSimilarity(model);
        UserNeighborhood neighborhood = new NearestNUserNeighborhood(getTotalUsers(), userSimilarity, model);
        Recommender recommender = new GenericUserBasedRecommender(model, neighborhood, userSimilarity);
        Recommender cachingRecommender = new CachingRecommender(recommender);
        int userId = userTraductor.get(userCode);
        List<RecommendedItem> recommendations = cachingRecommender.recommend(userId, getTotalProducts());
        List<String> l = new ArrayList();
        for (RecommendedItem recommendation : recommendations) {
            l.add(productTraductor.inverse().get((int) recommendation.getItemID()));
        }
        return l;
    }
}
