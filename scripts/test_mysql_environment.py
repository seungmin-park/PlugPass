import unittest

from mysql_environment import verification_environment


class MySqlEnvironmentTests(unittest.TestCase):
    def test_verification_cannot_inherit_external_database_or_provider_settings(self):
        parent = {
            'PATH': '/test/bin', 'CI': 'true',
            'SPRING_DATASOURCE_URL': 'jdbc:mysql://external.invalid/production',
            'SPRING_FLYWAY_URL': 'jdbc:mysql://external.invalid/production',
            'SPRING_APPLICATION_JSON': '{"spring":{"profiles":{"active":"production"}}}',
            'PLUGPASS_PUBLIC_DATA_SERVICE_KEY': 'synthetic-parent-key',
            'PLUGPASS_INGESTION_ENABLED': 'true', 'MANAGEMENT_SERVER_PORT': '8081',
            'DATA_API_KEY': 'synthetic-parent-key',
            'JAVA_TOOL_OPTIONS': '-Dspring.datasource.url=jdbc:mysql://external.invalid/production',
            'JDK_JAVA_OPTIONS': '-Dspring.flyway.url=jdbc:mysql://external.invalid/production',
            '_JAVA_OPTIONS': '-Dspring.profiles.active=production',
        }

        environment = verification_environment(parent)

        self.assertEqual(environment, {'PATH': '/test/bin', 'CI': 'true'})
        self.assertEqual(parent['SPRING_DATASOURCE_URL'], 'jdbc:mysql://external.invalid/production')
