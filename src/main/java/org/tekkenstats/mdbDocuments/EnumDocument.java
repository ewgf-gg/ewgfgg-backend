package org.tekkenstats.mdbDocuments;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Map;

@Data //lombok annotation for generating boilerplate (getters, setters, tostring, etc)
@Document(collection = "enums") //specify collection
public class EnumDocument {

    //this is the 'hashed' ID that mongodb uses to locate the collection within the database
    @Id
    private String id;

    private Map<String,String> fighters;

}
