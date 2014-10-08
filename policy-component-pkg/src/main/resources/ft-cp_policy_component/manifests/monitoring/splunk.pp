class cp_policy_component::monitoring::splunk {

  $environment = hiera('lifecycle_environment')

  splunk_client::saved_search {"Content_Platform_Policy_Component_Errors":
    username                => 'dashcontent',
    password                => 'd1shc0nt3nt',
    search_string           => "source=/var/log/apps/policy-component-dw-app.log level=ERROR",
    alert_condition         => 'search count > 0',
    cron_schedule           => '*/10 * * * *',
    search_range_start_time => "-15m",
    search_range_end_time   => "-5m",
    search_description      => "<h1>ERROR events on Content Platform Polocy Component hosts</h1><br/>Severity 3<br/><br/><strong>Business Impact</strong><br/>This component formats UP platform content for delivery to B2B clients, an outage will prevent those clients from accessing the content.<br/><br/><strong>Technical Impact</strong><br/>Requests via APIGee for B2B consumers will fail.",
    action_email_recepient  => "contentplatform.$environment.alerts@ft.com",
    classname               => "cp_policy_component" ;
  }
}
