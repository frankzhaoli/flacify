package com.shibu.flacify;

public class Song
{
    private int id, numplays, numlikes;
    private String title;

    public Song(String id, String title, String numplays, String numlikes)
    {
        this.id = Integer.parseInt(id);
        this.title = title;
        this.numplays = Integer.parseInt(numplays);
        this.numlikes = Integer.parseInt(numlikes);
        /*
        try
        {
            this.id = Integer.parseInt(id);
            this.title = title;
            this.numplays = Integer.parseInt(numplays);
            this.numlikes = Integer.parseInt(numlikes);
        }
        catch (Exception e)
        {
            this.id=0;
        }
        */
    }

    public int getId()
    {
        return id;
    }

    public String getTitle()
    {
        return title;
    }

    public int getNumplays()
    {
        return numplays;
    }

    public int getNumlikes()
    {
        return numlikes;
    }
}
