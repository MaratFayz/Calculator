create table authorities (
       id bigint not null,
       name varchar(255) not null,
       primary key (id),
       unique (name)
);

create table company (
       id bigint not null auto_increment,
       code varchar(10) not null,
       date_time_last_change datetime(6) not null,
       name varchar(255) not null,
       user_id bigint not null,
       primary key (id)
);

create table counterpartner (
       id bigint not null auto_increment,
       date_time_last_change datetime(6) not null,
       name varchar(255) not null,
       user_id bigint not null,
       primary key (id)
);

create table currency (
       id bigint not null auto_increment,
       cbrcurrency_code varchar(6),
       date_time_last_change datetime(6) not null,
       name varchar(255) not null,
       short_name varchar(3) not null,
       user_id bigint not null,
       primary key (id)
);

create table deposit_rate (
       end_period DATE not null,
       start_period DATE not null,
       rate decimal(19,2) not null,
       date_time_last_change datetime(6) not null,
       currency_id bigint not null,
       company_id bigint not null,
       scenario_id bigint not null,
       duration_id bigint not null,
       user_id bigint not null,

       primary key (end_period,
       start_period,
       company_id,
       currency_id,
       duration_id,
       scenario_id)
);

create table duration (
       id bigint not null auto_increment,
       max_month integer not null,
       min_month integer not null,
       date_time_last_change datetime(6) not null,
       name varchar(255) not null,
       user_id bigint not null,
       primary key (id)
);

create table end_date (
       end_date DATE not null,
       date_time_last_change datetime(6) not null,
       leasing_deposit_id bigint not null,
       scenario_id bigint not null,
       period_id bigint not null,
       user_id bigint not null,
       primary key (leasing_deposit_id,
       period_id,
       scenario_id)
);

create table entries (
       calculation_time datetime(6) not null,
       accum_amort_discont_end_period_rub_reg_ld_2_n DECIMAL(30,10) not null,
       accum_amort_discont_end_period_rub_reg_ld_3_s DECIMAL(30,10) not null,
       accum_amort_discont_end_period_cur_reg_ld_2_j DECIMAL(30,10) not null,
       accum_amort_discont_start_period_rub_reg_ld_2_k DECIMAL(30,10) not null,
       accum_amort_discont_start_period_rub_reg_ld_3_r DECIMAL(30,10) not null,
       accum_amort_discont_start_period_cur_reg_ld_2_h DECIMAL(30,10) not null,
       advance_currentperiod_reg_ld_3_ae DECIMAL(30,10) not null,
       advance_prevperiod_reg_ld_3_af DECIMAL(30,10) not null,
       amort_discont_current_period_rub_reg_ld_2_m DECIMAL(30,10) not null,
       amort_discont_current_period_cur_reg_ld_2_i DECIMAL(30,10) not null,
       corr_acc_amort_disc_rub_reg_ld_1_t DECIMAL(30,10) not null,
       corr_new_date_higher_corr_acc_amort_disc_rub_reg_ld_1_v DECIMAL(30,10) not null,
       corr_new_date_higher_discont_rub_reg_ld_1_u DECIMAL(30,10) not null,
       corr_new_date_less_corr_acc_amort_disc_rub_reg_ld_1_x DECIMAL(30,10) not null,
       corr_new_date_less_discont_rub_reg_ld_1_w DECIMAL(30,10) not null,
       discont_at_start_date_rub_reg_ld_1_l DECIMAL(30,10) not null,
       discont_at_start_date_rub_forifrsacc_reg_ld_1_m DECIMAL(30,10) not null,
       discont_at_start_date_cur_reg_ld_1_k DECIMAL(30,10) not null,
       discont_sum_at_new_end_date_cur_reg_ld_1_p DECIMAL(30,10) not null,
       disc_diff_betw_disconts_rub_reg_ld_1_r DECIMAL(30,10) not null,
       disc_sum_at_new_end_date_rub_reg_ld_1_q DECIMAL(30,10) not null,
       disposal_body_rub_reg_ld_3_x DECIMAL(30,10) not null,
       disposal_discont_rub_reg_ld_3_y DECIMAL(30,10) not null,
       incoming_ld_body_rub_reg_ld_3_l DECIMAL(30,10) not null,
       ldterm_reg_ld_3_z enum('ST','LT') not null,
       outcoming_ld_body_reg_ld_3_m DECIMAL(30,10) not null,
       reval_acc_amort_minus_rub_reg_ld_3_u DECIMAL(30,10) not null,
       reval_acc_amort_plus_rub_reg_ld_3_t DECIMAL(30,10) not null,
       reval_corr_disc_rub_reg_ld_1_s DECIMAL(30,10) not null,
       reval_ld_body_minus_reg_ld_3_o DECIMAL(30,10) not null,
       reval_ld_body_plus_reg_ld_3_n DECIMAL(30,10) not null,
       sum_minus_forex_diff_reg_ld_w DECIMAL(30,10) not null,
       sum_plus_forex_diff_reg_ld_3_v DECIMAL(30,10) not null,
       status_entry_made_during_or_after_closed_period enum('CURRENT_PERIOD', 'AFTER_CLOSING_PERIOD') not null,
       termreclass_body_currentperiod_reg_ld_3_aa DECIMAL(30,10) not null,
       termreclass_body_prevperiod_reg_ld_3_ac DECIMAL(30,10) not null,
       termreclass_percent_currentperiod_reg_ld_3_ab DECIMAL(30,10) not null,
       termreclass_percent_prevperiod_reg_ld_3_ad DECIMAL(30,10) not null,
       deposit_sum_not_disc_rub_reg_ld_1_n DECIMAL(30,10) not null,
       discounted_sum_at_current_end_date_cur_reg_ld_3_g DECIMAL(30,10) not null,
       end_date_at_this_period DATE not null,
       date_time_last_change datetime(6) not null,
       percent_rate_for_period_forld decimal(19,2) not null,
       transaction_status enum('ACTUAL', 'STORNO', 'DELETED'),
       leasing_deposit_id bigint not null,
       scenario_id bigint not null,
       period_id bigint not null,
       user_id bigint not null,

       primary key (calculation_time,
       leasing_deposit_id,
       period_id,
       scenario_id)
);

create table entryifrsacc (
       date_time_last_change datetime(6) not null,
       sum DECIMAL(30,10) not null,
       ifrs_account_id bigint not null,
       entry_calculation_time datetime(6) not null,
       entry_leasing_deposit_id bigint not null,
       entry_period_id bigint not null,
       entry_scenario_id bigint not null,
       user_id bigint not null,

       primary key (entry_calculation_time,
       entry_leasing_deposit_id,
       entry_period_id,
       entry_scenario_id,
       ifrs_account_id)
);

create table exchange_rate (
       date datetime(6) not null,
       average_rate_for_month DECIMAL(31,12),
       date_time_last_change datetime(6) not null,
       rate_at_date DECIMAL(31,12) not null,
       currency_id bigint not null,
       scenario_id bigint not null,
       user_id bigint not null,

       primary key (currency_id,
       date,
       scenario_id)
);

create table hibernate_sequence (next_val bigint);
insert into hibernate_sequence values ( 1 );
insert into hibernate_sequence values ( 1 );
insert into hibernate_sequence values ( 1 );

create table ifrs_account (
       id bigint not null auto_increment,
       account_code varchar(40),
       account_name varchar(255),
       ct varchar(255),
       dr varchar(255),
       flow_code varchar(255),
       flow_name varchar(255),
       is_inverse_sum bit not null,
       date_time_last_change datetime(6) not null,
       mapping_form_and_column varchar(255),
       pa varchar(255),
       sh varchar(255),
       user_id bigint not null,

       primary key (id)
);

create table leasing_deposits (
       id bigint not null auto_increment,
       deposit_sum_not_disc decimal(19,2) not null,
       is_created enum('X'),
       is_deleted enum('X'),
       date_time_last_change datetime(6) not null,
       start_date DATE not null,
       company_id bigint not null,
       counterpartner_id bigint not null,
       currency_id bigint not null,
       scenario_id bigint not null,
       user_id bigint not null,

       primary key (id)
);

create table period (
       id bigint not null auto_increment,
       date datetime(6) not null,
       date_time_last_change datetime(6) not null,
       user_id bigint not null,

       primary key (id),
       unique (date)
);

create table periods_closed (
       isclosed enum('X'),
       date_time_last_change datetime(6) not null,
       scenario_id bigint not null,
       period_id bigint not null,
       user_id bigint not null,

       primary key (period_id,
       scenario_id)
);

create table role_authorities (
       role_id bigint not null,
       authority_id bigint not null,

       primary key (role_id, authority_id)
);

create table roles (
       id bigint not null,
       date_time_last_change datetime(6) not null,
       name varchar(255) not null,
       user_changed_id bigint not null,

       primary key (id),
       unique (name)
);

create table scenario (
       id bigint not null auto_increment,
       is_blocked enum('X'),
       date_time_last_change datetime(6) not null,
       name varchar(255) not null,
       storno_status enum('FULL','ADDITION') not null,
       user_id bigint not null,

       primary key (id)
);

create table users (
       id bigint not null,
       is_account_expired enum('X'),
       is_credentials_expired enum('X'),
       is_enabled enum('X'),
       is_locked enum('X'),
       date_time_last_change datetime(6) not null,
       password varchar(255) not null,
       username varchar(255) not null,
       user_changed_id bigint,

       primary key (id),
       unique (username)
);

create table users_roles (
       user_id bigint not null,
       role_id bigint not null
);

alter table company
    add constraint company_users_fk
    foreign key (user_id) references users (id);

alter table counterpartner
    add constraint counterpartner_users_fk
    foreign key (user_id) references users (id);

alter table currency
    add constraint currency_users_fk
    foreign key (user_id) references users (id);

alter table duration
    add constraint duration_users_fk
    foreign key (user_id) references users (id);

alter table ifrs_account
    add constraint ifrs_account_users_fk
    foreign key (user_id) references users (id);

alter table period
    add constraint period_users_fk
    foreign key (user_id) references users (id);



alter table users_roles
    add constraint users_roles_users_fk
    foreign key (user_id) references users (id);

alter table users_roles
    add constraint users_roles_roles_fk
    foreign key (role_id) references roles (id);

alter table roles
    add constraint roles_users_id_fk
    foreign key (user_changed_id) references users (id);

alter table scenario
    add constraint users_scenario_fk
    foreign key (user_id) references users (id);

alter table users
    add constraint users_user_changed_fk
    foreign key (user_changed_id) references users (id);

alter table deposit_rate
    add constraint deposit_rate_users_fk
    foreign key (user_id) references users (id);

alter table deposit_rate
    add constraint deposit_rate_currency_fk
    foreign key (currency_id) references currency (id);

alter table deposit_rate
    add constraint deposit_rate_company_fk
    foreign key (company_id) references company (id);

alter table deposit_rate
    add constraint deposit_rate_scenario_fk
    foreign key (scenario_id) references scenario (id);

alter table deposit_rate
    add constraint deposit_rate_duration_fk
    foreign key (duration_id) references duration (id);

alter table end_date
    add constraint end_date_users_fk
    foreign key (user_id) references users (id);

alter table end_date
    add constraint end_date_scenario_fk
    foreign key (scenario_id) references scenario (id);

alter table end_date
    add constraint end_date_period_fk
    foreign key (period_id) references period (id);

alter table end_date
    add constraint end_date_leasing_deposit_fk
    foreign key (leasing_deposit_id) references leasing_deposits (id);

alter table entries
    add constraint entries_users_fk
    foreign key (user_id) references users (id);

alter table entries
    add constraint entries_scenario_fk
    foreign key (scenario_id) references scenario (id);

alter table entries
    add constraint entries_period_fk
    foreign key (period_id) references period (id);

alter table entries
    add constraint entries_leasing_deposits_fk
    foreign key (leasing_deposit_id) references leasing_deposits (id);

alter table entryifrsacc
    add constraint entryifrsacc_ifrs_account_fk
    foreign key (ifrs_account_id) references ifrs_account (id);

alter table entryifrsacc
    add constraint entryifrsacc_entry_fk
    foreign key (entry_calculation_time,
       entry_leasing_deposit_id,
       entry_period_id,
       entry_scenario_id) references entries (calculation_time,
       leasing_deposit_id,
       period_id,
       scenario_id);

alter table entryifrsacc
    add constraint entryifrsacc_users_fk
    foreign key (user_id) references users (id);

alter table exchange_rate
    add constraint exchange_rate_users
    foreign key (user_id) references users (id);

alter table exchange_rate
    add constraint exchange_rate_currency
    foreign key (currency_id) references currency (id);

alter table exchange_rate
    add constraint exchange_rate_scenario
    foreign key (scenario_id) references scenario (id);

alter table leasing_deposits
    add constraint leasing_deposits_users
    foreign key (user_id) references users (id);

alter table leasing_deposits
    add constraint leasing_deposits_companies
    foreign key (company_id) references company (id);

alter table leasing_deposits
    add constraint leasing_deposits_counterpartners
    foreign key (counterpartner_id) references counterpartner (id);

alter table leasing_deposits
    add constraint leasing_deposits_currencies
    foreign key (currency_id) references currency (id);

alter table leasing_deposits
    add constraint leasing_deposits_scenarios
    foreign key (scenario_id) references scenario (id);

alter table periods_closed
    add constraint periods_closed_users
    foreign key (user_id) references users (id);

alter table periods_closed
    add constraint periods_closed_scenarios
    foreign key (scenario_id) references scenario (id);

alter table periods_closed
    add constraint periods_closed_period
    foreign key (period_id) references period (id);

alter table role_authorities
    add constraint role_authorities_authorities
    foreign key (authority_id) references authorities (id);

alter table role_authorities
    add constraint role_authorities_roles
    foreign key (role_id) references roles (id);