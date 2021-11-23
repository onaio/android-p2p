package com.trevorgowing.android.p2p

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.trevorgowing.android.p2p.databinding.FragmentP2pStatusBinding

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class P2pStatusFragment : Fragment() {

  private var _binding: FragmentP2pStatusBinding? = null

  // This property is only valid between onCreateView and
  // onDestroyView.
  private val binding get() = _binding!!

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentP2pStatusBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val peerDeviceRecyclerView: RecyclerView =
      view.findViewById(R.id.wifi_p2p_peer_devices_recycler_view)
    peerDeviceRecyclerView.adapter = WifiP2pDeviceAdapter(emptyList())
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}