package org.jahia.se.jcustomer.plugins.game_4_jcustomer_plugin.actions;

import org.apache.unomi.api.Event;
import org.apache.unomi.api.Profile;
import org.apache.unomi.api.actions.Action;
import org.apache.unomi.api.actions.ActionExecutor;
import org.apache.unomi.api.services.EventService;
import org.apache.unomi.api.services.ProfileService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SetOrUpdateDistinctObjectValuesAction implements ActionExecutor {
    private static Logger logger = LoggerFactory.getLogger(SetOrUpdateDistinctObjectValuesAction.class);
    private ProfileService service;

    class Score {
        private Logger logger = LoggerFactory.getLogger(Score.class);
        private String key;
        private String score;
        private final String fallbackPattern="::";
        private final String splitPattern;

        public Score(String key, String score, String splitPattern){
            this.key=key;
            this.score=score;
            this.splitPattern=(splitPattern != null && !splitPattern.isEmpty()) ? splitPattern : fallbackPattern;
        }
        public Score(String arg, String splitPattern){
            this.splitPattern=(splitPattern != null && !splitPattern.isEmpty()) ? splitPattern : fallbackPattern;

            String[] record = arg.split(this.splitPattern);
            if(record.length != 2)
                throw new IllegalArgumentException("arg must respect the format <key>-><score> : " + arg);

            this.key=record[0];
            this.score=record[1];
        }
        public String getKy(){
            return this.key;
        }
        public String getScore(){
            return this.score;
        }
        public void setKey(String key){
            this.key=key;
        }
        public void setScore(String score){
            this.score=score;
        }
        public String toString(){
            return this.key+splitPattern+this.score;
        }
        @Override
        public boolean equals(Object o){
            if(!(o instanceof Score))
                return false;
            Score score = (Score) o;
            logger.debug("equals -> this.key.equals(score.key) : "+this.key+".equals("+score.key+")");
            return this.key.equals(score.key);
        }
    }

    public int execute(Action action, Event event) {
        logger.debug("start !");
        final Profile profile = event.getProfile();
        List<Score> scoreValues = new ArrayList<>();

        String propertyName = (String) action.getParameterValues().get("setPropertyName");
        logger.debug("propertyName :"+propertyName);
        //null is managed in Score class
        String splitPattern = (String) action.getParameterValues().get("setSplitPattern");
        logger.debug("splitPattern :"+splitPattern);

//        PropertyType propertyType = service.getPropertyType(propertyName);
        //check if property exist, if not return.
        if(service.getPropertyType(propertyName) == null)
            return EventService.NO_CHANGE;

        Score propertyValue = new Score( (String) action.getParameterValues().get("setPropertyValue"),splitPattern );
        logger.debug("propertyValue :"+propertyValue);

        //by default I manage here only user profile properties
        List<String> values = (List<String>) profile.getProperty(propertyName);
        if(values == null)
            values = new ArrayList<>();
        logger.debug("values :"+values);

        scoreValues = values.stream().map(value -> new Score(value,splitPattern)).collect(Collectors.toList());
        logger.debug("scoreValues :"+scoreValues);

        //look for current value id, if exist update it else create new one
        profile.setProperty(propertyName, updateDistinctValues(scoreValues,propertyValue));

        return EventService.PROFILE_UPDATED;
    }

    //used to update profile service
    public void setProfileService(ProfileService service) {
        this.service = service;
    }

    private List<String> updateDistinctValues(List<Score> values, Score propertyValue){

        logger.debug("getDistinctValues -> values : "+values);
        logger.debug("getDistinctValues -> propertyValue : "+propertyValue);

        int index = values.indexOf(propertyValue);

        if(index > -1){
            Score score = values.get(index);
            score.setScore(propertyValue.getScore());
        }else{
            values.add(propertyValue);
        }
        return values.stream().map(score -> score.toString()).collect(Collectors.toList());
    };
}
