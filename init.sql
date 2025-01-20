-- Initial setup
SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

-- Table Definitions
CREATE TABLE public.aggregated_statistics (
    game_version integer NOT NULL,
    character_id character varying NOT NULL,
    dan_rank integer NOT NULL,
    category character varying NOT NULL,
    region_id integer NOT NULL,
    area_id integer NOT NULL,
    total_wins integer,
    total_losses integer,
    total_players integer,
    total_replays integer,
    computed_at timestamp without time zone
);

CREATE TABLE public.battles (
    battle_id character varying NOT NULL,
    date character varying NOT NULL,
    battle_at bigint NOT NULL,
    battle_type integer,
    game_version integer NOT NULL,
    player1_id character varying NOT NULL,
    player1_polaris_id character varying,
    player1_character_id integer NOT NULL,
    player1_name character varying,
    player1_region integer,
    player1_area integer,
    player1_language character varying,
    player1_tekken_power bigint NOT NULL,
    player1_dan_rank integer NOT NULL,
    player1_rating_before integer,
    player1_rating_change integer,
    player1_rounds_won integer NOT NULL,
    player2_id character varying NOT NULL,
    player2_polaris_id character varying,
    player2_character_id integer NOT NULL,
    player2_name character varying,
    player2_region integer,
    player2_area integer,
    player2_language character varying,
    player2_tekken_power bigint NOT NULL,
    player2_dan_rank integer NOT NULL,
    player2_rating_before integer,
    player2_rating_change integer,
    player2_rounds_won integer NOT NULL,
    stageid integer NOT NULL,
    winner integer NOT NULL
);

CREATE TABLE public.character_stats (
   player_id character varying NOT NULL,
   character_id character varying NOT NULL,
   dan_rank integer NOT NULL,
   game_version integer NOT NULL,
   latest_battle bigint,
   wins integer,
   losses integer
);

CREATE TABLE public.enums (
  id integer NOT NULL,
  enum_type character varying(40),
  enum_key integer,
  enum_value character varying(40)
);

CREATE SEQUENCE public.enums_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.enums_id_seq OWNED BY public.enums.id;

CREATE TABLE public.past_player_names (
   id bigint NOT NULL,
   name character varying(255) NOT NULL,
   player_id character varying NOT NULL
);

CREATE SEQUENCE public.past_player_names_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.past_player_names_id_seq OWNED BY public.past_player_names.id;

CREATE TABLE public.players (
    player_id character varying NOT NULL,
    polaris_id character varying,
    name character varying,
    region_id integer,
    area_id integer,
    language character varying,
    latest_battle bigint,
    tekken_power bigint
);

CREATE TABLE public.tekken_stats_summary (
    id integer NOT NULL,
    total_replays bigint,
    total_players bigint
);

-- Default values
ALTER TABLE ONLY public.enums ALTER COLUMN id SET DEFAULT nextval('public.enums_id_seq'::regclass);
ALTER TABLE ONLY public.past_player_names ALTER COLUMN id SET DEFAULT nextval('public.past_player_names_id_seq'::regclass);

-- Primary Keys and Constraints
ALTER TABLE ONLY public.aggregated_statistics
    ADD CONSTRAINT aggregated_statistics_pkey PRIMARY KEY (game_version, character_id, dan_rank, category, region_id, area_id);

ALTER TABLE ONLY public.battles
    ADD CONSTRAINT battles_pkey PRIMARY KEY (battle_id);

ALTER TABLE ONLY public.character_stats
    ADD CONSTRAINT character_stats_pkey PRIMARY KEY (player_id, character_id, game_version);

ALTER TABLE ONLY public.enums
    ADD CONSTRAINT enums_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.past_player_names
    ADD CONSTRAINT past_player_names_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.past_player_names
    ADD CONSTRAINT past_player_names_player_id_name_key UNIQUE (player_id, name);

ALTER TABLE ONLY public.players
    ADD CONSTRAINT players_pkey PRIMARY KEY (player_id);

ALTER TABLE ONLY public.tekken_stats_summary
    ADD CONSTRAINT tekken_stats_summary_pkey PRIMARY KEY (id);

-- Indexes
CREATE INDEX idx_battle_at ON public.battles USING btree (battle_at);
CREATE INDEX idx_name ON public.players USING btree (name);
CREATE INDEX idx_player1_id ON public.battles USING btree (player1_id);
CREATE INDEX idx_player2_id ON public.battles USING btree (player2_id);
CREATE INDEX idx_polaris_id ON public.players USING btree (polaris_id);

-- Foreign Keys
ALTER TABLE ONLY public.character_stats
    ADD CONSTRAINT character_stats_player_id_fkey FOREIGN KEY (player_id) REFERENCES public.players(player_id);

ALTER TABLE ONLY public.past_player_names
    ADD CONSTRAINT past_player_names_player_id_fkey FOREIGN KEY (player_id) REFERENCES public.players(player_id) ON DELETE CASCADE;