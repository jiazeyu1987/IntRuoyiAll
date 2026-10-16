"""Endpoint-observation regression fixtures; no live source or S3 acceptance."""
import copy
from pathlib import Path
import shutil
import subprocess
import sys
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from formal_capture_semantics import parse_ip_address_output, validate_endpoint_observation
from formal_snapshot_linux import FormalSnapshotError


class CaptureEndpointTests(unittest.TestCase):
    def setUp(self):
        self.source = dict(endpoint='http://172.21.0.4:9000', minioContainerId='a'*64, containerPort=9000)
        self.observed = dict(id='a'*64, running=True, ports={}, networks={'selected-bridge': dict(
            NetworkID='b'*64, EndpointID='c'*64, IPAddress='172.21.0.4', GlobalIPv6Address='')})
        self.interfaces = [dict(addr_info=[dict(local='127.0.0.1'), dict(local='172.21.0.1')])]

    def verify(self):
        return validate_endpoint_observation(self.source, self.observed, self.interfaces)

    def rejected(self, code):
        with self.assertRaises(FormalSnapshotError) as error: self.verify()
        self.assertEqual(code, error.exception.code)

    def test_explicit_bridge_address_with_empty_published_ports(self):
        before = copy.deepcopy(self.source)
        self.assertEqual('CONTAINER', self.verify())
        self.assertEqual(before, self.source)

    def test_container_address_does_not_need_host_interface_assignment(self):
        self.interfaces = []
        self.assertEqual('CONTAINER', self.verify())

    def test_container_address_uses_internal_port_even_with_host_publication(self):
        self.observed['ports'] = {'9000/tcp': [dict(HostIp='0.0.0.0', HostPort='19000')]}
        self.assertEqual('CONTAINER', self.verify())

    def test_explicit_container_ipv6_address(self):
        self.source['endpoint'] = 'http://[fd00:21::4]:9000'
        self.observed['networks']['selected-bridge']['GlobalIPv6Address'] = 'fd00:21::4'
        self.assertEqual('CONTAINER', self.verify())

    def test_wrong_container_id(self):
        self.observed['id'] = 'd'*64
        self.rejected('FORMAL_CAPTURE_SEMANTIC_ENDPOINT_SOURCE_CHANGED')

    def test_stopped_container(self):
        self.observed['running'] = False
        self.rejected('FORMAL_CAPTURE_SEMANTIC_ENDPOINT_SOURCE_CHANGED')

    def test_wrong_internal_port_does_not_select_published_route(self):
        self.source['endpoint'] = 'http://172.21.0.4:19000'
        self.observed['ports'] = {'9000/tcp': [dict(HostIp='0.0.0.0', HostPort='19000')]}
        self.interfaces[0]['addr_info'].append(dict(local='172.21.0.4'))
        self.rejected('FORMAL_CAPTURE_SEMANTIC_ENDPOINT_CONTAINER_PORT_CHANGED')

    def test_address_not_assigned_to_selected_container(self):
        self.observed['networks']['selected-bridge']['IPAddress'] = '172.21.0.5'
        self.rejected('FORMAL_CAPTURE_SEMANTIC_ENDPOINT_MAPPING_REQUIRED')

    def test_foreign_container_address_cannot_use_wildcard_host_binding(self):
        self.observed['networks']['selected-bridge']['IPAddress'] = '172.21.0.5'
        self.observed['ports'] = {'9000/tcp': [dict(HostIp='0.0.0.0', HostPort='9000')]}
        self.rejected('FORMAL_CAPTURE_SEMANTIC_ENDPOINT_NOT_LOCAL')

    def test_ambiguous_network_assignment(self):
        self.observed['networks']['other-bridge'] = copy.deepcopy(self.observed['networks']['selected-bridge'])
        self.rejected('FORMAL_CAPTURE_SEMANTIC_ENDPOINT_SOURCE_CHANGED')

    def test_missing_network_identity(self):
        self.observed['networks']['selected-bridge']['EndpointID'] = ''
        self.rejected('FORMAL_CAPTURE_SEMANTIC_ENDPOINT_NETWORK_INVALID')

    def test_malformed_network_address(self):
        self.observed['networks']['selected-bridge']['IPAddress'] = 'not-an-address'
        self.rejected('FORMAL_CAPTURE_SEMANTIC_ENDPOINT_NETWORK_INVALID')

    def test_explicit_port_still_required(self):
        self.source['endpoint'] = 'http://172.21.0.4'
        self.rejected('FORMAL_CAPTURE_SEMANTIC_ENDPOINT_PORT_REQUIRED')

    def test_dns_name_is_not_resolved_or_substituted(self):
        self.source['endpoint'] = 'http://minio:9000'
        self.rejected('FORMAL_CAPTURE_SEMANTIC_ENDPOINT_NATIVE_ADDRESS_REQUIRED')

    def test_original_explicit_host_published_port(self):
        self.source['endpoint'] = 'http://172.21.0.1:19000'
        self.observed['ports'] = {'9000/tcp': [dict(HostIp='0.0.0.0', HostPort='19000')]}
        self.assertEqual('PUBLISHED', self.verify())

    def test_host_route_still_requires_published_port(self):
        self.source['endpoint'] = 'http://172.21.0.1:19000'
        self.rejected('FORMAL_CAPTURE_SEMANTIC_ENDPOINT_MAPPING_REQUIRED')

    def test_host_route_rejects_wrong_publication(self):
        self.source['endpoint'] = 'http://172.21.0.1:19000'
        self.observed['ports'] = {'9000/tcp': [dict(HostIp='0.0.0.0', HostPort='9000')]}
        self.rejected('FORMAL_CAPTURE_SEMANTIC_ENDPOINT_SOURCE_CHANGED')


class IpAddressObservationTests(unittest.TestCase):
    RAW = (b'1: lo    inet 127.0.0.1/8 scope host lo\\       valid_lft forever preferred_lft forever\n'
        b'1: lo    inet6 ::1/128 scope host \\       valid_lft forever preferred_lft forever\n'
        b'2: eth0@if7    inet 172.21.0.1/16 brd 172.21.255.255 scope global eth0\\       valid_lft forever preferred_lft forever\n'
        b'2: eth0@if7    inet6 fe80::1/64 scope link \\       valid_lft forever preferred_lft forever\n')

    def test_ipv4_ipv6_and_interface_identity(self):
        interfaces = parse_ip_address_output(self.RAW)
        self.assertEqual([(1, 'lo'), (2, 'eth0@if7')], [(i['ifindex'], i['ifname']) for i in interfaces])
        self.assertEqual(['127.0.0.1', '::1', '172.21.0.1', 'fe80::1'],
            [a['local'] for i in interfaces for a in i['addr_info']])

    def test_published_route_consumes_actual_parsed_host_address(self):
        source = dict(endpoint='http://172.21.0.1:19000', minioContainerId='a'*64, containerPort=9000)
        observed = dict(id='a'*64, running=True, networks={},
            ports={'9000/tcp': [dict(HostIp='0.0.0.0', HostPort='19000')]})
        self.assertEqual('PUBLISHED', validate_endpoint_observation(source, observed, parse_ip_address_output(self.RAW)))
        source['endpoint'] = 'http://172.21.0.9:19000'
        with self.assertRaises(FormalSnapshotError) as error:
            validate_endpoint_observation(source, observed, parse_ip_address_output(self.RAW))
        self.assertEqual('FORMAL_CAPTURE_SEMANTIC_ENDPOINT_NOT_LOCAL', error.exception.code)

    def test_malformed_or_incomplete_observation_is_rejected(self):
        samples = (b'', b'Option -j is unknown\n', b'{}\n', b'\xff\n', self.RAW[:-1],
            b'1: lo inet 127.0.0.1 scope host lo\n', b'1: lo inet 999.1.1.1/8 scope host lo\n',
            b'1: lo inet6 127.0.0.1/8 scope host lo\n', b'1: lo inet 127.0.0.1/999 scope host lo\n',
            b'1: lo inet 127.0.0.1/255.0.0.0 scope host lo\n', b'1: lo inet 127.0.0.1/8 dynamic scope\n',
            b'1: lo inet 127.0.0.1/8 scope host lo\x00\n',
            b'1: lo inet 127.0.0.1/8 scope host lo\r\n',
            self.RAW+b'2: other inet 10.0.0.1/8 scope global other\n')
        for raw in samples:
            with self.subTest(raw=raw), self.assertRaises(FormalSnapshotError) as error:
                parse_ip_address_output(raw)
            self.assertEqual('FORMAL_CAPTURE_SEMANTIC_ENDPOINT_OBSERVATION_FAILED', error.exception.code)

    @unittest.skipUnless(sys.platform.startswith('linux') and shutil.which('ip'), 'Requires the actual local Linux ip tool')
    def test_real_local_ip_one_line_observation(self):
        # Read-only local tool check; no backup server, source container or business data.
        observed = subprocess.run(['ip', '-o', 'address', 'show'], capture_output=True, timeout=5, check=False)
        self.assertEqual(0, observed.returncode)
        self.assertEqual(b'', observed.stderr)
        interfaces = parse_ip_address_output(observed.stdout)
        self.assertTrue(interfaces)


if __name__ == '__main__': unittest.main()
