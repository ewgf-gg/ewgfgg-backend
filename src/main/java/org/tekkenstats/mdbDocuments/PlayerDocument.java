package org.tekkenstats.mdbDocuments;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Map;

@Data //lombok annotation for generating boilerplate (getters, setters, tostring, etc)
@Document(collection = "player-data") //specify collection
public class PlayerDocument {

    //ID annotation corresponds to '_id:' field in mongoDB Database. Will set this to unique ID
    @Id
    private String id;

    private Map<String,String> playerData;

}
