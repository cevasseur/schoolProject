/**
 * @file TunnelReduction.h
 * @author Vincent Penelle (vincent.penelle@u-bordeaux.fr)
 * @brief An implementation of the reduction of the Tunnel Routing problem to SAT. Converts a network n and a bound b to a propositional formula that is satisfiable if and only there is a well-formed simple path of size b from the source to the target. A satisfying valuation represents such a path.
 * Provides functions to generate the formula, the necessary variables, and decoding a path from a valuation.
 * @version 0.1
 * @date 2025-10-03
 *
 * @copyright Creative Commons
 *
 */

#ifndef TUNNEL_RED_H
#define TUNNEL_RED_H

#include "TunnelNetwork.h"
#include <z3.h>

/**
 * @brief Generates a propositional formula satisfiable if and only if there is a well-formed simple path of size @p bound from the initial node of @p network to its final node.
 *
 * @param ctx The solver context.
 * @param network A Tunnel Network.
 * @param length The size of the target path.
 * @return Z3_ast The formula
 * @pre @p network must be initialized.
 */
Z3_ast tn_reduction(Z3_context ctx, const TunnelNetwork network, int length);

/**
 * @brief Gets the well-formed path from the model @p model.
 *
 * @param ctx The solver context.
 * @param model A variable assignment.
 * @param network A Tunnel Network.
 * @param bound The size of the path.
 * @param path The path
 * @pre @p path must be an array of size @p bound+1.
 */
void tn_get_path_from_model(Z3_context ctx, Z3_model model, TunnelNetwork network, int bound, tn_step *path);

/**
 * @brief Prints (in pretty format) which variables used by the tunnel reduction are true in @p model.
 *
 * @param ctx The solver context.
 * @param model A variable assignment.
 * @param network A tunnel network.
 * @param bound The size of the path.
 */
void tn_print_model(Z3_context ctx, Z3_model model, TunnelNetwork network, int bound);

/**
 * @brief Check every position in the path to be sure that if there is a node, the pile can't have a 4 and a 6 at the same time
 * @param ctx The solver context.
 * @param network A Tunnel Network.
 * @param l The size of the target path.
 * @param hmax The max size of the stack.
 * @return Z3_ast
 * @pre @p network must be initialized.
 */
Z3_ast pile_p1(Z3_context ctx, TunnelNetwork network, int l, int hmax);

/**
 * @brief Check for each height under the current node that there is another and the stack isn't weird
 * @param ctx The solver context.
 * @param network A Tunnel Network.
 * @param h The height we are looking from.
 * @param n The node we currently are.
 * @param p The position we currently are.
 * @return Z3_ast
 * @pre @p network must be initialized.
 */
Z3_ast B(Z3_context ctx, TunnelNetwork network, int h, int n, int p);

/**
 * @brief Check for each height over the current node that there is nothing
 * @param ctx The solver context.
 * @param network A Tunnel Network.
 * @param h The height we are looking from.
 * @param n The node we currently are.
 * @param p The position we currently are.
 * @param hmax The maximum height of the stack.
 * @return Z3_ast
 * @pre @p network must be initialized.
 */
Z3_ast A(Z3_context ctx, TunnelNetwork network, int h, int hmax, int n, int p);

/**
 * @brief Check the stack has the right value on his top after an operation
 * @param ctx The solver context.
 * @param network A Tunnel Network.
 * @param l The size of the target path.
 * @param hmax The max size of the stack.
 * @return Z3_ast
 * @pre @p network must be initialized.
 */
Z3_ast pile_p2(Z3_context ctx, TunnelNetwork network, int l, int hmax);

/**
 * @brief Check the start and the end of the path.
 * @param ctx The solver context.
 * @param network A Tunnel Network.
 * @param l The size of the target path.
 * @param hmax The max size of the stack.
 * @return Z3_ast 
 * @pre @p network must be initialized.
 */
Z3_ast start_end_condition(Z3_context ctx, TunnelNetwork network, int hmax, int l);

/**
 * @brief Check if there is a simple path of size l in the network
 * @param ctx The solver context.
 * @param network A Tunnel Network.
 * @param l The size of the target path.
 * @param hmax The max size of the stack.
 * @return Z3_ast
 * @pre @p network must be initialized.
 */
Z3_ast simple_path(Z3_context ctx, TunnelNetwork network, int hmax, int l);

/**
 * @brief Our implementation of At_most
 * @param ctx The solver context.
 * @param network A Tunnel Network.
 * @param l The size of the target path.
 * @param hmax The max size of the stack.
 * @return Z3_ast The formula
 * @pre @p network must be initialized.
 */
Z3_ast My_At_Most(Z3_context ctx, TunnelNetwork network, int hmax, int l);

/**
 * @brief Check if the node can make an operation to another node.
 *
 * @param ctx The solver context.
 * @param network The network we are working in.
 * @param node The current node.
 * @param pos The path position.
 * @param height The height of the cell described.
 * @param bound The maximum size of the pile.
 * @return Z3_ast
 */
Z3_ast valid_path(Z3_context ctx, TunnelNetwork network, int node, int pos, int h, int bound);

/**
 * @brief Each stage of the stack as the same value at a position and the next one (not last h if pop)
 * @param ctx The solver context.
 * @param network A Tunnel Network.
 * @param pos The position we currently are.
 * @param h The height of the stack.
 * @return Z3_ast
 * @pre @p network must be initialized.
 */
Z3_ast equivalent(Z3_context ctx, int pos, int h);

/**
 * @brief Verifying a node transmit correctly
 * @param ctx The solver context.
 * @param a The transmitting value.
 * @param pos The position we currently are.
 * @param h The current height of the stack.
 * @param current The node doing the transmission.
 * @param next A different node comparing to current.
 * @return Z3_ast The formula
 * @pre @p network must be initialized.
 */
Z3_ast transmitting(Z3_context ctx, int a, int pos, int h, int next, int current);

/**
 * @brief Verifying a node push correctly
 * @param ctx The solver context.
 * @param a The current value of the top of the stack.
 * @param b The pushing value.
 * @param pos The position we currently are.
 * @param h The current height of the stack.
 * @param current The node doing the pushing.
 * @param next A different node comparing to current.
 * @param bound The maximum size of the stack.
 * @return Z3_ast
 * @pre @p network must be initialized.
 */
Z3_ast pushing(Z3_context ctx, int a, int b, int pos, int h, int next, int bound, int current);

/** 
 * @brief Verifying a node pop correctly
 * @param ctx The solver context.
 * @param a The current value of the top of the stack.
 * @param b The popping value.
 * @param pos The position we currently are.
 * @param h The current height of the stack.
 * @param current The node doing the popping.
 * @param next A different node comparing to current.
 * @return Z3_ast
 * @pre @p network must be initialized.
 */
Z3_ast popping(Z3_context ctx, int a, int b, int pos, int h, int next, int current);

#endif